package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.lang3.StringUtils;

public class JdkReleases implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String JAVA_RELEASES_CNF_FILENAME = "java_releases_v1.yml";
  private static final String REMOTE_XML = "https://raw.githubusercontent.com/takari/jdkget/master/src/main/resources/" + JAVA_RELEASES_CNF_FILENAME;
  private static final long MAX_CACHE = 24L * 60L * 60L * 1000L; // cache it for a day
  private static final int TIMEOUT_VALUE = 10000;

  private static final Object mutex = new Object();

  private static volatile JdkReleases cached;
  private static volatile long time;

  public enum JavaReleaseType {
    JDK("jdk"),
    JRE("jre"),
    SERVERJRE("serverjre");
    
    private String name; 
    JavaReleaseType(String name) {
      this.name = name;
    }
    
    public String getName(){
      return name;
    }
    
    public static JavaReleaseType getDefault() {
      return JDK;
    }

    public static boolean contains(String typeName) {
      return Arrays.asList(JavaReleaseType.values()).stream()
          .anyMatch(t -> StringUtils.equals(typeName, t.getName()));
    }
    
    public static List<String> names(){
      return Arrays.asList(JavaReleaseType.values()).stream()
          .map(t -> t.getName())
          .collect(Collectors.toList());
    }

    public static List<String> validateTypeNames(String[] typeNames){
      return validateTypeNames(typeNames == null ? null : Arrays.asList(typeNames), 
          Arrays.asList(JavaReleaseType.getDefault().getName()));
    }
    
    public static List<String> validateTypeNames(List<String> typeNames){
      return validateTypeNames(typeNames, 
          Arrays.asList(JavaReleaseType.getDefault().getName()));
    }
    
    public static List<String> validateTypeNames(List<String> types, List<String> defaultTypeNames) {
      if(types == null) {
        return defaultTypeNames;
      }
      
      List<String> res = types.stream().filter(t -> JavaReleaseType.contains(t))
          .collect(Collectors.toList());
      return res.size() < 1 ? defaultTypeNames : res;
    }

    public static String validateTypeName(String typeName) {
      return validateTypeName(typeName, getDefault().getName());
    }
    
    public static String validateTypeName(String typeName, String defaultType) {
      if(StringUtils.isEmpty(typeName) || !contains(typeName)) {
        return defaultType;
      }
      return typeName;
    }
  }
  
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
    try (InputStream in = JdkReleases.class.getClassLoader()
        .getResourceAsStream(JAVA_RELEASES_CNF_FILENAME)) {
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
    private final Map<String, Map<Arch, JdkBinary>> binaries;

    JdkRelease(JdkVersion version, boolean psu, Map<String, List<JdkBinary>> binaries) {
      this.version = version;
      this.psu = psu;
      
      Map<String, Map<Arch, JdkBinary>> binMap = new LinkedHashMap<>();
      binaries.entrySet().forEach(e -> {
        binMap.put(e.getKey(), Collections.unmodifiableMap(toMap(e.getValue())));
      });
      
      this.binaries = Collections.unmodifiableMap(binMap);
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

    public JdkBinary getBinary(String type, Arch arch) {
      JdkBinary b = binaries.get(type).get(arch);
      if (b == null) {
        throw new IllegalStateException("No binary for " + arch + " in " + version);
      }
      return b;
    }

    public Set<Arch> getArchs(String type) {
      return binaries.get(type).keySet();
    }
    
    public Set<String> getTypes(List<String> allowedTypes) {
      return binaries.keySet().stream()
          .filter(t -> allowedTypes.stream().anyMatch(at -> at.equals(t)))
          .collect(Collectors.toSet());
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
    return newBuilder(rels, null);
  }
  
  public static Builder newBuilder(JdkReleases rels, List<String> typeNames) {
    Builder b = new Builder();
    List<String> releaseTypes = JavaReleaseType.validateTypeNames(typeNames);
    for (JdkRelease r : rels.getReleases()) {
      String v = r.getVersion().shortBuild();
      if (r.isPsu()) {
        b.setPSU(v);
      }
      
      r.binaries.entrySet().stream()
      .filter( e -> releaseTypes.contains(e.getKey()))
      .forEach(typedBins -> {
        typedBins.getValue().values().stream().forEach(bin -> 
          b.addBinary(v, typedBins.getKey(), bin.getArch(), bin.getPath(), bin.getMd5(),
            bin.getSha256(), bin.getSize())
        );
      });
    }

    for (JCE jce : rels.jces) {
      b.addJCE(jce.getMajorVersion(), jce.getPath());
    }

    return b;
  }

  public static class Builder {
    private Map<String, Map<String,List<JdkBinary>>> binaries;
    private List<JCE> jces;
    private Set<String> psuVersions;

    public Builder() {
      binaries = new LinkedHashMap<>();
      jces = new ArrayList<>();
    }

    public Builder addBinary(String version, Arch arch, String path) {
      return addBinary(version, null, arch, path, null, null, -1);
    }
    
    public Builder addBinary(String version, String type, Arch arch, String path) {
      return addBinary(version, type, arch, path, null, null, -1);
    }

    public Builder addBinary(String version, Arch arch, String path, String md5, String sha256, long size) {
      return addBinary(version, null, arch, path, md5, sha256, size);
    }
    
    public Builder addBinary(String version, String type, Arch arch, String path, String md5, String sha256, long size) {
      String binType = JavaReleaseType.validateTypeName(type);
      Map<String, List<JdkBinary>> bv = binaries.get(version);
      if (bv == null) {
        binaries.put(version, bv = new LinkedHashMap<>());
      }
      List<JdkBinary> bt = bv.get(binType);
      if (bt == null) {
        bv.put(binType, bt = new ArrayList<>());
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

      binaries.entrySet().stream().forEach(e -> 
        rels.add(new JdkRelease(JdkVersion.parse(e.getKey()), //version 
            psuVersions != null && psuVersions.contains(e.getKey()), //psu
            e.getValue()))//bins
      );

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
