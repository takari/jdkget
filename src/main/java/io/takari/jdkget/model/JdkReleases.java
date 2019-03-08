package io.takari.jdkget.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.takari.jdkget.Arch;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.ITransportFactory;
import io.takari.jdkget.StdOutput;

public class JdkReleases implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String JAVA_RELEASES_CNF_FILENAME = "java_releases_v1.yml";
  public static final String ORACLE_RELEASES =
      "https://raw.githubusercontent.com/takari/jdkget/master/src/main/resources/" + JAVA_RELEASES_CNF_FILENAME;
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
      output.error("Warning: Unable to retreive " + JAVA_RELEASES_CNF_FILENAME +
          " from Github. Using built-in JDK list.");
      return readFromClasspath();
    }
  }

  public static JdkReleases readFromGithub() throws IOException {
    return readFromUrl(ORACLE_RELEASES);
  }

  public static JdkReleases readFromUrl(String url) throws IOException {
    URLConnection conn = new URL(url).openConnection();
    if (conn instanceof HttpURLConnection) {
      ((HttpURLConnection) conn).setRequestMethod("GET");
    }

    conn.setAllowUserInteraction(false);
    conn.setDoInput(true);
    conn.setDoOutput(false);
    conn.setUseCaches(true);
    conn.setConnectTimeout(TIMEOUT_VALUE);
    conn.setReadTimeout(TIMEOUT_VALUE);
    conn.connect();
    return read(conn.getInputStream());
  }

  public static final JdkReleases readFromClasspath() throws IOException {
    try (InputStream in = JdkReleases.class.getClassLoader()
        .getResourceAsStream(JAVA_RELEASES_CNF_FILENAME)) {
      return new JdkReleasesParser().parse(in);
    }
  }

  public static JdkReleases read(InputStream inputStream) throws IOException {
    return new JdkReleasesParser().parse(inputStream);
  }

  private String transport;
  private List<JdkRelease> releases;
  private List<JCE> jces;

  JdkReleases(String transport, List<JdkRelease> releases, List<JCE> jces) {
    this.transport = transport;
    this.releases = releases;
    this.jces = jces;
  }

  public String getTransport() {
    return transport;
  }

  public ITransportFactory createTransportFactory() {
    try {
      return (ITransportFactory) Class.forName(getTransport()).newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
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

  public JdkRelease select(String ver) {
    return ver == null || ver.equals("latest") ? latest() : select(JdkVersion.parse(ver));
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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(JdkReleases rels) {
    Builder b = new Builder();
    b.transport(rels.getTransport());
    for (JdkRelease r : rels.getReleases()) {
      String v = r.getVersion().shortBuild();
      if (r.isPsu()) {
        b.setPSU(v);
      }

      r.binaries.forEach((type, typeBins) -> {
        typeBins.forEach((arch, bins) -> {
          bins.forEach(bin -> {
            b.addBinary(v, type, arch, //
                bin.getPath(), bin.getMd5(), bin.getSha256(), bin.getSize());
          });
        });
      });
    }

    for (JCE jce : rels.jces) {
      b.addJCE(jce.getMajorVersion(), jce.getPath());
    }

    return b;
  }

  public static class Builder {
    private String transport;
    private Map<String, Map<BinaryType, List<JdkBinary>>> binaries;
    private List<JCE> jces;
    private Set<String> psuVersions;

    public Builder() {
      binaries = new LinkedHashMap<>();
      jces = new ArrayList<>();
    }

    public Builder transport(String transport) {
      this.transport = transport;
      return this;
    }

    public Builder addBinary(String version, Arch arch, String path) {
      return addBinary(version, arch, path, null, null, -1);
    }

    public Builder addBinary(String version, BinaryType type, Arch arch, String path) {
      return addBinary(version, type, arch, path, null, null, -1);
    }

    public Builder addBinary(String version, Arch arch, String path, String md5, String sha256, long size) {
      return addBinary(version, null, arch, path, md5, sha256, size);
    }

    public Builder addBinary(String version, BinaryType type, Arch arch, String path, String md5, String sha256, long size) {
      Map<BinaryType, List<JdkBinary>> bv = binaries.get(version);
      if (bv == null) {
        binaries.put(version, bv = new LinkedHashMap<>());
      }
      List<JdkBinary> bt = bv.get(type);
      if (bt == null) {
        bv.put(type, bt = new ArrayList<>());
      }
      bt.add(new JdkBinary(arch, path, md5, sha256, size));
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

      binaries.entrySet().stream().forEach(e -> rels.add(new JdkRelease(JdkVersion.parse(e.getKey()), // version
          psuVersions != null && psuVersions.contains(e.getKey()), // psu
          e.getValue()))// bins
      );

      Collections.sort(rels, new Comparator<JdkRelease>() {
        @Override
        public int compare(JdkRelease r1, JdkRelease r2) {
          return r2.getVersion().compareTo(r1.getVersion());
        }
      });

      return new JdkReleases(transport, rels, jces);
    }

  }

}
