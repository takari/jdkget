package io.takari.jdkget;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.base.Throwables;

import io.takari.jdkget.extract.BinJDKExtractor;
import io.takari.jdkget.extract.OsxJDKExtractor;
import io.takari.jdkget.extract.TZJDKExtractor;
import io.takari.jdkget.extract.TgzJDKExtractor;
import io.takari.jdkget.extract.WindowsJDKExtractor;
import io.takari.jdkget.extract.ZipJDKExtractor;
import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.BinaryType;
import io.takari.jdkget.model.JdkBinary;
import io.takari.jdkget.model.JdkRelease;
import io.takari.jdkget.model.JdkReleases;
import io.takari.jdkget.model.JdkVersion;
import io.takari.jdkget.oracle.OracleWebsiteTransport;

public class JdkGetter {

  public static final int DEFAULT_RETRIES = 3;

  public static final int CONNECTION_REQUEST_TIMEOUT = 1 * 1000;
  public static final int CONNECT_TIMEOUT = 30 * 1000;
  public static final int SOCKET_TIMEOUT = 2 * 60 * 1000;

  private final ITransport transport;
  private final IOutput log;

  private boolean removeDownloads = true;
  private boolean silent = false;
  private int retries = DEFAULT_RETRIES;
  private int socketTimeout = SOCKET_TIMEOUT;
  private int connectTimeout = CONNECT_TIMEOUT;
  private int connectionRequestTimeout = CONNECTION_REQUEST_TIMEOUT;

  public JdkGetter(ITransport transport, IOutput log) {
    this.transport = transport == null ? new OracleWebsiteTransport() : transport;
    this.log = log == null ? StdOutput.INSTANCE : log;
  }

  public ITransport getTransport() {
    return transport;
  }

  public IOutput getLog() {
    return log;
  }

  public boolean isRemoveDownloads() {
    return removeDownloads;
  }

  public void setRemoveDownloads(boolean removeDownloads) {
    this.removeDownloads = removeDownloads;
  }

  public boolean isSilent() {
    return silent;
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getConnectionRequestTimeout() {
    return connectionRequestTimeout;
  }

  public void setConnectionRequestTimeout(int connectionRequestTimeout) {
    this.connectionRequestTimeout = connectionRequestTimeout;
  }

  public void getJdk(File outputDirectory) throws IOException, InterruptedException {
    getJdk(null, outputDirectory);
  }

  public void getJdk(Arch arch, File outputDirectory) throws IOException, InterruptedException {
    getJdk(null, null, arch, outputDirectory);
  }

  public void getJdk(JdkRelease rel, JCE jce, Arch arch, File outputDirectory)
      throws IOException, InterruptedException {
    get(rel, jce, arch, null, outputDirectory);
  }

  public void get(JdkRelease rel, JCE jce, Arch arch, BinaryType type, File outputDirectory)
      throws IOException, InterruptedException {

    if (rel == null) {
      rel = JdkReleases.get().latest();
    }
    if (arch == null) {
      arch = Arch.autodetect();
    }
    if (type == null) {
      type = BinaryType.getDefault();
    }

    JdkVersion theVersion = rel.getVersion();
    String versionDesc = theVersion.shortBuild() + " for " + arch.toString().toLowerCase().replace("_", "");
    getLog().info("Getting jdk " + versionDesc);
    JdkBinary bin = rel.getUnpackableBinary(type, arch);
    if (bin == null) {
      throw new IOException("Can't find matching binary for " + versionDesc);
    } else {
      getLog().info("  Found matching binary: " + bin.getPath());
    }

    File jdkImage = new File(outputDirectory.getParentFile(), new File(bin.getPath()).getName());
    File jceImage = null;
    boolean jceFix = false;
    if (jce != null && theVersion.major < 9) {
      if (theVersion.major == 8 && theVersion.minor >= 151) {
        jceFix = true;
      } else {
        jceImage = new File(jdkImage.getParentFile(), new File(jce.getPath()).getName());
      }
    }

    boolean valid = false;
    int retr = retries;

    while (!valid) {
      boolean dontRetry = retr <= 0;
      try {
        if (jdkImage.exists()) {
          if (transport.validate(this, bin, jdkImage)) {
            getLog().info("We already have a valid copy of " + jdkImage);
          } else {
            getLog().info("Found existing invalid image");
            FileUtils.forceDelete(jdkImage);
          }
        }

        if (!jdkImage.exists()) {
          transport.downloadJdk(this, bin, jdkImage);
        }

        if (!jdkImage.exists()) {
          getLog().error("Cannot download jdk " + theVersion.shortBuild() + " for " + arch);
          throw new IOException("Transport failed to download jdk image");
        }

        getLog().info("Validating downloaded image");

        Util.checkInterrupt();
        valid = transport.validate(this, bin, jdkImage);
      } catch (InterruptedException e) {
        throw e;
      } catch (Exception e) {
        if (dontRetry) {
          Throwables.propagateIfPossible(e, IOException.class);
          throw Throwables.propagate(e);
        }
        getLog().error("Error getting jdk: " + e.toString() + ", retrying..");
        valid = false;
      }

      if (!valid && dontRetry) {
        break;
      }
      retr--;
    }
    if (!valid) {
      throw new IOException("Transport downloaded invalid image");
    }

    IJdkExtractor extractor = getExtractor(jdkImage);
    getLog().info("Using extractor " + extractor.getClass().getSimpleName());
    if (!extractor.extractJdk(this, bin, jdkImage, outputDirectory)) {
      throw new IOException("Failed to extract JDK from " + jdkImage);
    }

    File jdkHome = outputDirectory;
    boolean libFound = new File(jdkHome, "lib").isDirectory();
    if (!libFound) {
      File osxHome = new File(jdkHome, "Contents/Home");
      if (new File(osxHome, "lib").isDirectory()) {
        jdkHome = osxHome;
        libFound = true;
      }
    }
    if (!libFound) {
      throw new IOException("Cannot detect jdk installation");
    }

    if (jceImage != null && !jceImage.exists()) {
      transport.downloadJce(this, jce, jceImage);
      new JCEExtractor().extractJCE(this, type, jceImage, jdkHome);
    }
    if (jceFix) {
      new JCEExtractor().fixJce(this, type, jdkHome);
    }

    // rebuild jsa cache (https://docs.oracle.com/javase/9/vm/class-data-sharing.htm)
    // but only if we're running on a compatible system (usually we do)
    if (arch == Arch.autodetect()) {
      rebuildJsa(arch, jdkHome);
    }

    if (removeDownloads) {
      if (jdkImage.exists()) {
        FileUtils.forceDelete(jdkImage);
      }
      if (jceImage != null && jceImage.exists()) {
        FileUtils.forceDelete(jceImage);
      }
    }
  }

  private void rebuildJsa(Arch arch, File jdkHome) throws IOException, InterruptedException {
    getLog().info("Building JSA cache");
    try {
      String cmd = new File(jdkHome, arch.isWindows() ? "bin\\java.exe" : "bin/java").getAbsolutePath();
      Process proc = new ProcessBuilder(cmd, "-Xshare:dump")
          .directory(jdkHome)
          .redirectErrorStream(true)
          .start();

      InputStream in = proc.getInputStream();
      List<String> stdout = IOUtils.readLines(in, Charset.defaultCharset());
      int ret = proc.waitFor();
      if (ret != 0) {
        getLog().info("Ignoring an error building JSA cache:");
        for (String l : stdout) {
          getLog().info(" > " + l);
        }
      }
    } catch (Throwable t) {
      Throwables.propagateIfInstanceOf(t, InterruptedException.class);
      getLog().info("Ignoring an error building JSA cache: " + t);
    }
  }

  private static IJdkExtractor getExtractor(File jdkImage) {
    String name = jdkImage.getName().toLowerCase();
    if (name.endsWith(".tar.gz")) {
      return new TgzJDKExtractor();
    }
    if (name.endsWith(".tar.z")) {
      return new TZJDKExtractor();
    }
    if (name.endsWith(".zip")) {
      return new ZipJDKExtractor();
    }
    if (name.endsWith(".bin")) {
      return new BinJDKExtractor();
    }
    if (name.endsWith(".dmg")) {
      return new OsxJDKExtractor();
    }
    if (name.endsWith(".exe")) {
      return new WindowsJDKExtractor();
    }
    throw new IllegalArgumentException("Cannot select extractor impl for " + name);
  }

}
