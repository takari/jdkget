package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

public class JdkReleases implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String REMOTE_XML = "https://raw.githubusercontent.com/takari/jdkget/master/src/main/resources/jdkreleases.xml";
  private static final long MAX_CACHE = 24L * 60L * 60L * 1000L; // cache it for a day
  private static final int TIMEOUT_VALUE = 10000;

  private static final Object mutex = new Object();
  private static volatile JdkReleases cached;
  private static volatile long time;

  public static JdkReleases get() throws IOException {
    return get(StdOutput.INSTANCE);
  }

  public static JdkReleases get(IOutput output) throws IOException {
    JdkReleases c = cached;
    long t = System.currentTimeMillis() - MAX_CACHE;
    if (c == null || t > time) {
      synchronized (mutex) {
        if (c == null || t > time) {
          cached = c = readCached(output);
          time = System.currentTimeMillis();
        }
      }
    }
    return c;
  }

  private static final JdkReleases readCached(IOutput output) throws IOException {
    if ("builtin".equals(System.getProperty("io.takari.jdkget.releaseList"))) {
      return readFromClasspath();
    }

    try {
      return readFromGithub();
    } catch (Exception e) {
      output.error("Warning: Unable to retreive jdkreleases.xml from Github. Using built-in JDK list.");
      return readFromClasspath();
    }
  }

  public static JdkReleases readFromGithub() throws IOException {
    HttpsURLConnection conn = (HttpsURLConnection) new URL(REMOTE_XML).openConnection();
    conn.setAllowUserInteraction(false);
    conn.setDoInput(true);
    conn.setDoOutput(false);
    conn.setUseCaches(true);
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(TIMEOUT_VALUE);
    conn.setReadTimeout(TIMEOUT_VALUE);
    conn.connect();
    return read(conn.getInputStream());
  }

  public static final JdkReleases readFromClasspath() throws IOException {
    try (InputStream in = JdkReleases.class.getClassLoader().getResourceAsStream("jdkreleases.xml")) {
      return new JdkReleasesParser().parse(in);
    }
  }

  public static JdkReleases read(InputStream inputStream) throws IOException {
    return new JdkReleasesParser().parse(inputStream);
  }

  private List<JdkRelease> releases;
  private List<JCE> jces;

  JdkReleases(List<JdkRelease> releases, List<JCE> jces) {
    this.releases = releases;
    this.jces = jces;
  }

  public JCE getJCE(JdkVersion ver) {
    if (jces != null) {
      for (JCE jce : jces) {
        if (jce.getMajorVersion() == ver.major) {
          return jce;
        }
      }
    }
    return null;
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
      if (ver.minor == -1) {
        if (rel.isPsu()) {
          continue;
        }
        return rel;
      }
      if (o.minor > ver.minor) {
        continue;
      }
      if (o.minor < ver.minor) {
        break;
      }
      if (ver.security == -1 && o.security != -1) {
        if (rel.isPsu()) {
          continue;
        }
        return rel;
      }
      if (o.security > ver.security) {
        continue;
      }
      if (o.security < ver.security) {
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

  public static class JCE implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int majorVersion;
    private final String path;

    public JCE(int majorVersion, String path) {
      this.majorVersion = majorVersion;
      this.path = path;
    }

    public int getMajorVersion() {
      return majorVersion;
    }

    public String getPath() {
      return path;
    }

  }

  public static class JdkRelease implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JdkVersion version;
    private final boolean psu;
    private final Map<Arch, JdkBinary> binaries;

    JdkRelease(JdkVersion version, boolean psu, List<JdkBinary> binaries) {
      this.version = version;
      this.psu = psu;
      this.binaries = Collections.unmodifiableMap(toMap(binaries));
    }

    private Map<Arch, JdkBinary> toMap(List<JdkBinary> binaries) {
      Map<Arch, JdkBinary> binMap = new LinkedHashMap<>();
      for (JdkBinary binary : binaries) {
        binMap.put(binary.getArch(), binary);
      }
      return binMap;
    }

    public JdkVersion getVersion() {
      return version;
    }

    public boolean isPsu() {
      return psu;
    }

    public JdkBinary getBinary(Arch arch) {
      JdkBinary b = binaries.get(arch);
      if (b == null) {
        throw new IllegalStateException("No binary for " + arch + " in " + version);
      }
      return b;
    }

    public Set<Arch> getArchs() {
      return binaries.keySet();
    }
  }

  public static class JdkBinary implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Arch arch;
    private final String path;
    private final String md5;
    private final String sha256;
    private final long size;

    JdkBinary(Arch arch, String path, String md5, String sha256, long size) {
      this.arch = arch;
      this.path = path;
      this.md5 = md5;
      this.sha256 = sha256;
      this.size = size;
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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(JdkReleases rels) {
    Builder b = new Builder();

    for (JdkRelease r : rels.getReleases()) {
      String v = r.getVersion().shortBuild();
      if (r.isPsu()) {
        b.setPSU(v);
      }
      for (JdkBinary bin : r.binaries.values()) {
        b.addBinary(v, bin.getArch(), bin.getPath(), bin.getMd5(), bin.getSha256(), bin.getSize());
      }
    }

    for (JCE jce : rels.jces) {
      b.addJCE(jce.getMajorVersion(), jce.getPath());
    }

    return b;
  }

  public static class Builder {
    private Map<String, List<JdkBinary>> binaries;
    private List<JCE> jces;
    private Set<String> psuVersions;

    public Builder() {
      binaries = new LinkedHashMap<>();
      jces = new ArrayList<>();
    }

    public Builder addBinary(String version, Arch arch, String path) {
      return addBinary(version, arch, path, null, null, -1);
    }

    public Builder addBinary(String version, Arch arch, String path, String md5, String sha256, long size) {
      List<JdkBinary> bl = binaries.get(version);
      if (bl == null) {
        binaries.put(version, bl = new ArrayList<>());
      }
      bl.add(new JdkBinary(arch, path, md5, sha256, size));
      return this;
    }

    public Builder setPSU(String version) {
      if (psuVersions == null) {
        psuVersions = new HashSet<>();
      }
      psuVersions.add(version);
      return this;
    }

    public Builder addJCE(int major, String path) {
      jces.add(new JCE(major, path));
      return this;
    }

    public JdkReleases build() {
      List<JdkRelease> rels = new ArrayList<>();

      for (Map.Entry<String, List<JdkBinary>> e : binaries.entrySet()) {
        JdkVersion v = JdkVersion.parse(e.getKey());
        List<JdkBinary> bins = e.getValue();
        boolean psu = psuVersions != null && psuVersions.contains(e.getKey());
        rels.add(new JdkRelease(v, psu, bins));
      }

      Collections.sort(rels, new Comparator<JdkRelease>() {
        @Override
        public int compare(JdkRelease r1, JdkRelease r2) {
          return r2.getVersion().compareTo(r1.getVersion());
        }
      });

      return new JdkReleases(rels, jces);
    }
  }
}
