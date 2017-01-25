package io.takari.jdkget;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import io.takari.jdkget.JdkGetter.JdkVersion;

public class JdkReleases {

  private static final String REMOTE_XML = "https://raw.githubusercontent.com/takari/jdkget/master/src/main/resources/jdkreleases.xml";
  private static final long MAX_CACHE = 24L * 60L * 60L * 1000L; // cache it for a day
  private static final int TIMEOUT_VALUE = 10000;

  private static final Object mutex = new Object();
  private static volatile JdkReleases cached;

  public static JdkReleases get() throws IOException {
    JdkReleases c = cached;
    long t = System.currentTimeMillis() - MAX_CACHE;
    if (c == null || t > c.time) {
      synchronized (mutex) {
        if (c == null || t > c.time) {
          cached = c = read();
        }
      }
    }
    return c;
  }

  private static final JdkReleases read() throws IOException {
    try {
        HttpsURLConnection conn = (HttpsURLConnection) new URL(REMOTE_XML).openConnection();
        conn.setAllowUserInteraction( false );
        conn.setDoInput( true );
        conn.setDoOutput( false );
        conn.setUseCaches( true );
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_VALUE);
        conn.setReadTimeout(TIMEOUT_VALUE);
        conn.connect();

        return new JdkReleasesParser().parse(conn.getInputStream());
    }
    catch (Exception e) {
        System.err.println("Warning: Unable to retreive jdkreleases.xml from Github. Using default JDK list.");
        e.printStackTrace();
        InputStream in = JdkReleases.class.getClassLoader().getResourceAsStream("jdkreleases.xml");

        return new JdkReleasesParser().parse(in);
    }
  }

  private List<JdkRelease> releases;
  private long time;

  JdkReleases(List<JdkRelease> releases) {
    this.releases = releases;
    time = System.currentTimeMillis();
  }

  public List<JdkRelease> getReleases() {
    return releases;
  }

  public JdkRelease latest() {
    return releases.stream().filter(r -> !r.isPsu()).findAny().get();
  }
  public JdkRelease latestInclPSU() {
    return releases.get(0);
  }

  public JdkRelease select(JdkVersion ver) {
    for (JdkRelease rel : releases) {
      JdkVersion o = rel.getVersion();
      if (o.major > ver.major) {
        continue;
      }
      if (o.major < ver.major) {
        break;
      }
      if (ver.revision == -1) {
        if (rel.isPsu()) {
          continue;
        }
        return rel;
      }
      if (o.revision > ver.revision) {
        continue;
      }
      if (o.revision < ver.revision) {
        break;
      }
      if (ver.buildNumber.isEmpty()) {
        return rel;
      }
      if (o.buildNumber.equals(ver.buildNumber)) {
        return rel;
      }
    }
    throw new IllegalStateException("Unable to find jdk release for version " + ver);
  }

  public static class JdkRelease {
    private final JdkVersion version;
    private final boolean psu;
    private final Map<Arch, JdkBinary> binaries;

    public JdkRelease(JdkVersion version, boolean psu) {
      this.version = version;
      this.psu = psu;
      this.binaries = new HashMap<>();
    }

    void addBinary(JdkBinary binary) {
      binaries.put(binary.getArch(), binary);
    }

    public JdkVersion getVersion() {
      return version;
    }

    public boolean isPsu() {
      return psu;
    }

    public JdkBinary getBinary(Arch arch) {
      JdkBinary b = binaries.get(arch);
      if(b == null) {
        throw new IllegalStateException("No binary for " + arch + " in " + version);
      }
      return b;
    }
    
    public Set<Arch> getArchs() {
      return Collections.unmodifiableSet(binaries.keySet());
    }
  }

  public static class JdkBinary {
    private final JdkRelease release;
    private final Arch arch;
    private final String path;
    private final String md5;
    private final String sha256;
    private final long size;

    public JdkBinary(JdkRelease release, Arch arch, String path, String md5, String sha256, long size) {
      this.release = release;
      this.arch = arch;
      this.path = path;
      this.md5 = md5;
      this.sha256 = sha256;
      this.size = size;
    }

    public JdkRelease getRelease() {
      return release;
    }

    public Arch getArch() {
      return arch;
    }

    public String getPath() {
      return path;
    }

    public String getMd5() {
      return md5;
    }

    public String getSha256() {
      return sha256;
    }
    
    public long getSize() {
      return size;
    }
  }
}
