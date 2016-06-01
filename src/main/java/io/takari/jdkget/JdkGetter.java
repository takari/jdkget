package io.takari.jdkget;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.io.ByteStreams;
import com.sprylab.xar.XarEntry;
import com.sprylab.xar.XarFile;

import io.takari.jdkget.osx.PosixModes;
import io.takari.jdkget.osx.UnHFS;

public class JdkGetter {

  public static final String JDK_URL_FORMAT = "http://download.oracle.com/otn-pub/java/jdk/%su%s-%s/jdk-%su%s-macosx-x64.dmg";
  public static final String OTN_COOKIE = "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie";

  private final File outputDirectory;
  private final File inProcessDirectory;
  private final String jdkVersion;
  private final File jdkDmg;
  private final String downloadUrl;

  public JdkGetter(String version, File outputDirectory) {
    this.outputDirectory = outputDirectory;
    this.inProcessDirectory = new File(outputDirectory.getAbsolutePath() + ".in-process");
    String[] p = StringUtils.split(version, "_");
    String major = StringUtils.split(p[0], ".")[1];
    String revisionWithBuildNumber = p[1];
    String[] x = StringUtils.split(revisionWithBuildNumber, "-");
    String revision = x[0];
    String buildNumber = x[1];
    this.jdkVersion = String.format("jdk-%su%s", major, revision);
    this.jdkDmg = new File(inProcessDirectory, jdkVersion + "-macosx-x64.dmg");
    this.downloadUrl = String.format(JDK_URL_FORMAT, major, revision, buildNumber, major, revision);
  }

  public void get() throws Exception {
    if (!inProcessDirectory.exists()) {
      inProcessDirectory.mkdirs();
    }
    if (!jdkDmg.exists()) {
      System.out.println("Downloading " + downloadUrl);
      downloadJdkDmg();
    } else {
      System.out.println("We already have a copy of " + jdkDmg);
    }
    processDmg(jdkDmg, jdkVersion);
  }

  private void downloadJdkDmg() throws Exception {

    // Oracle does some redirects so we have to follow a couple before we win the JDK prize
    String url = downloadUrl;
    URL jdkUrl;
    int response = 0;
    HttpURLConnection connection;
    for (int retry = 0; retry < 4; retry++) {
      jdkUrl = new URL(url);
      connection = (HttpURLConnection) jdkUrl.openConnection();
      connection.setRequestProperty("Cookie", OTN_COOKIE);
      response = connection.getResponseCode();
      if (response == 200) {
        try (InputStream is = new BufferedInputStream(connection.getInputStream()); OutputStream os = new BufferedOutputStream(new FileOutputStream(jdkDmg))) {
          ByteStreams.copy(is, os);
        }
        break;
      } else if (response == 302) {
        url = connection.getHeaderField("Location");
      }
    }
  }

  public void processDmg(File jdkDmg, String jdkVersion) throws Exception {

    UnHFS.main(new String[] {
        "-o",
        inProcessDirectory.getAbsolutePath(),
        jdkDmg.getAbsolutePath()});

    List<File> files = FileUtils.getFiles(inProcessDirectory, "**/*.pkg", null, true);
    // validate
    File jdkPkg = files.get(0);
    XarFile xarFile = new XarFile(jdkPkg);
    for (XarEntry entry : xarFile.getEntries()) {
      if (!entry.isDirectory() && entry.getName().startsWith("jdk") && entry.getName().endsWith("Payload")) {
        File file = new File(inProcessDirectory, entry.getName());
        file.getParentFile().mkdirs();
        try (InputStream is = entry.getInputStream(); OutputStream os = new FileOutputStream(file)) {
          ByteStreams.copy(is, os);
        }
      }
    }

    files = FileUtils.getFiles(inProcessDirectory, "**/Payload", null, true);
    File jdkGz = files.get(0);
    File cpio = new File(inProcessDirectory, jdkVersion + ".cpio");
    try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(jdkGz)); FileOutputStream os = new FileOutputStream(cpio)) {
      ByteStreams.copy(is, os);
    }

    // https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt
    try (ArchiveInputStream is = new CpioArchiveInputStream(new FileInputStream(cpio))) {
      CpioArchiveEntry e;
      while ((e = (CpioArchiveEntry) is.getNextEntry()) != null) {
        if (!e.isDirectory()) {
          File jdkFile = new File(outputDirectory, e.getName());
          jdkFile.getParentFile().mkdirs();
          if (e.isRegularFile()) {
            try (OutputStream os = new FileOutputStream(jdkFile)) {
              ByteStreams.copy(is, os);
            }
          } else if (e.isSymbolicLink()) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
              ByteStreams.copy(is, os);
              Files.createSymbolicLink(jdkFile.toPath(), Paths.get(new String(os.toByteArray())));
            }            
          }
          // The lower 9 bits specify read/write/execute permissions for world, group, and user following standard POSIX conventions.            
          int mode = (int) e.getMode() & 0000777;
          Files.setPosixFilePermissions(jdkFile.toPath(), PosixModes.intModeToPosix(mode));
        }
      }
    }
    FileUtils.deleteDirectory(inProcessDirectory);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String version;
    private File outputDirectory;

    public JdkGetter build() {
      return new JdkGetter(version, outputDirectory);
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder outputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }
  }

  // DMG <-- XAR <-- GZ <-- CPIO
  public static void main(String[] args) throws Exception {
    String version = "1.8.0_92-b14";
    File jdkDir = new File("/Users/jvanzyl/js/DEVPROD/TOOLS/jdks/jdk-8u92");
    JdkGetter getter = JdkGetter.builder()
      .version(version)
      .outputDirectory(jdkDir)
      .build();

    getter.get();
  }

}
