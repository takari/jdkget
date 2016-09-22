package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Throwables;

import io.takari.jdkget.JdkReleases.JdkRelease;
import io.takari.jdkget.extract.BinJDKExtractor;
import io.takari.jdkget.extract.OsxJDKExtractor;
import io.takari.jdkget.extract.TZJDKExtractor;
import io.takari.jdkget.extract.WindowsJDKExtractor;

public class JdkGetter {

  private final JdkVersion jdkVersion;
  private final Arch arch;
  private final File outputDirectory;
  private final File inProcessDirectory;
  private final int retries;
  private ITransport transport;
  private IOutput output;

  public JdkGetter(String version, Arch arch, File outputDirectory, int retries, ITransport transport, IOutput output) {
    this(version == null || version.equals("latest") ? null : JdkVersion.parse(version), arch, outputDirectory, retries, transport, output);
  }

  public JdkGetter(JdkVersion jdkVersion, Arch arch, File outputDirectory, int retries, ITransport transport, IOutput output) {
    this.jdkVersion = jdkVersion;
    this.retries = retries;
    if (output != null) {
      this.output = output;
    } else {
      this.output = new StdOutput();
    }

    if (arch != null) {
      this.arch = arch;
    } else {
      this.arch = Arch.autodetect();
      this.output.info("Autodetected arch: " + this.arch);
    }
    if (transport != null) {
      this.transport = transport;
    } else {
      this.transport = new OracleWebsiteTransport();
    }
    this.outputDirectory = outputDirectory.getAbsoluteFile();
    this.inProcessDirectory = new File(outputDirectory.getPath() + ".in-process");
  }

  public Arch getArch() {
    return arch;
  }

  public JdkVersion getJdkVersion() {
    return jdkVersion;
  }

  public void get() throws IOException {
    if (!inProcessDirectory.exists()) {
      inProcessDirectory.mkdirs();
    }
    
    JdkVersion theVersion;
    if(jdkVersion != null) {
      theVersion = JdkReleases.get().select(jdkVersion).getVersion();
    } else {
      theVersion = JdkReleases.get().latest().getVersion();
    }
    
    output.info("Getting jdk " + theVersion.shortBuild() + " for " + arch.toString().toLowerCase().replace("_", ""));
    
    File jdkImage = transport.getImageFile(inProcessDirectory, arch, theVersion);

    boolean valid = false;
    int retr = retries;

    while (!valid) {
      boolean dontRetry = retr <= 0;
      try {
        if (jdkImage.exists()) {
          if (transport.validate(arch, theVersion, jdkImage, output)) {
            output.info("We already have a valid copy of " + jdkImage);
          } else {
            output.info("Found existing invalid image");
            FileUtils.forceDelete(jdkImage);
          }
        }

        if (!jdkImage.exists()) {
          transport.downloadJdk(arch, theVersion, jdkImage, output);
        }

        if (!jdkImage.exists()) {
          output.error("Cannot download jdk " + theVersion.shortBuild() + " for " + arch);
          throw new IOException("Transport failed to download jdk image");
        }
        
        output.info("Validating downloaded image");
        valid = transport.validate(arch, theVersion, jdkImage, output);
      } catch (Exception e) {
        if (dontRetry) {
          Throwables.propagateIfPossible(e, IOException.class);
          throw Throwables.propagate(e);
        }
        output.error("Error getting jdk: " + e.toString() + ", retrying..");
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
    output.info("Using extractor " + extractor.getClass().getSimpleName());
    if (!extractor.extractJdk(theVersion, jdkImage, outputDirectory, inProcessDirectory, output)) {
      throw new IOException("Failed to extract JDK from " + jdkImage);
    }
    FileUtils.deleteDirectory(inProcessDirectory);
  }

  private static IJdkExtractor getExtractor(File jdkImage) {
    String name = jdkImage.getName().toLowerCase();
    if (name.endsWith(".tar.gz")) {
      return new TgzJDKExtractor();
    }
    if (name.endsWith(".dmg")) {
      return new OsxJDKExtractor();
    }
    if (name.endsWith(".exe")) {
      return new WindowsJDKExtractor();
    }
    if (name.endsWith(".bin")) {
      return new BinJDKExtractor();
    }
    if (name.endsWith(".tar.z")) {
      return new TZJDKExtractor();
    }
    throw new IllegalArgumentException("Cannot select extractor impl for " + name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private JdkVersion jdkVersion;
    private String version;
    private Arch arch;
    private File outputDirectory;
    private int retries = 0;
    private ITransport transport;
    private IOutput output;

    public JdkGetter build() {
      if (jdkVersion != null) {
        return new JdkGetter(jdkVersion, arch, outputDirectory, retries, transport, output);
      }
      return new JdkGetter(version, arch, outputDirectory, retries, transport, output);
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder version(JdkVersion version) {
      this.jdkVersion = version;
      return this;
    }

    public Builder arch(Arch arch) {
      this.arch = arch;
      return this;
    }

    public Builder outputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder retries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder transport(ITransport transport) {
      this.transport = transport;
      return this;
    }

    public Builder output(IOutput output) {
      this.output = output;
      return this;
    }
  }

  public static class JdkVersion implements Comparable<JdkVersion> {

    public final int major;

    public final int revision;

    public final String buildNumber;

    private JdkVersion(int major, int revision, String buildNumber) {
      this.major = major;
      this.revision = revision;
      this.buildNumber = buildNumber;
    }

    public String toString() {
      return longBuild();
    }

    public String longVersion() {
      StringBuilder sb = new StringBuilder();
      sb.append("1.").append(major).append(".0");
      if (revision > 0)
        sb.append('_').append(revision);
      return sb.toString();
    }

    public String longBuild() {
      StringBuilder sb = new StringBuilder();
      sb.append("1.").append(major).append(".0");
      if (revision > 0)
        sb.append('_').append(revision);
      sb.append(buildNumber);
      return sb.toString();
    }

    public String shortVersion() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (revision > 0)
        sb.append('u').append(revision);
      return sb.toString();
    }

    public String shortBuild() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (revision > 0)
        sb.append('u').append(revision);
      sb.append(buildNumber);
      return sb.toString();
    }

    public static JdkVersion parse(String version) {
      if (version.startsWith("1.")) { // 1.8.0_91-b14
        String[] p = StringUtils.split(version, "_");
        String major = StringUtils.split(p[0], ".")[1];

        String revisionWithBuildNumber = p[1];
        String[] x = StringUtils.split(revisionWithBuildNumber, "-");
        String revision = x[0];
        String buildNumber = x.length > 1 ? "-" + x[1] : "";
        return new JdkVersion(i(major), i(revision), buildNumber);
      }

      if (version.contains("u")) { // 8u91-b14
        String[] p = StringUtils.split(version, "u");
        String major = p[0];

        String revisionWithBuildNumber = p[1];

        String[] x = StringUtils.split(revisionWithBuildNumber, "-");
        String revision = x[0];

        String buildNumber;
        if (revision.endsWith("b")) { // 6u5b
          buildNumber = revision.substring(revision.length() - 1);
          revision = revision.substring(0, revision.length() - 1);
        } else {
          buildNumber = x.length > 1 ? "-" + x[1] : "";
        }
        return new JdkVersion(i(major), i(revision), buildNumber);
      }

      if (version.contains("-")) { //8-b132
        String[] x = StringUtils.split(version, "-");
        String major = x[0];
        String buildNumber = x.length > 1 ? "-" + x[1] : "";
        return new JdkVersion(i(major), -1, buildNumber);
      }

      return new JdkVersion(i(version), -1, ""); // 7

      //throw new IllegalArgumentException("Unsupported version format: " + version);
    }

    private static int i(String s) {
      return Integer.parseInt(s);
    }

    int buildNum() {
      String b = buildNumber;
      if (b.startsWith("-")) {
        b = b.substring(1);
      }
      if (b.startsWith("b")) {
        b = b.substring(1);
      }
      if (b.isEmpty()) {
        return 0;
      }
      return Integer.parseInt(b);
    }

    @Override
    public int compareTo(JdkVersion o) {
      int c = major - o.major;
      if (c == 0) {
        c = revision - o.revision;
      }
      if (c == 0) {
        c = buildNum() - o.buildNum();
      }
      return c;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof JdkVersion) {
        JdkVersion that = (JdkVersion) obj;
        return major == that.major && revision == that.revision;
      }
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
    }

  }

  public static void main(String[] args) throws Exception {

    Options options = new Options();
    options.addOption("o", true, "Output dir");
    options.addOption("v", true, "JDK Version");
    options.addOption("a", true, "Architecture");
    options.addOption("l", false, "Lis versions");
    options.addOption("?", "help", false, "Help");

    CommandLine cli = new PosixParser().parse(options, args);

    if (cli.hasOption("l")) {

      System.out.println("Available JDK versions:");
      for (JdkRelease r : JdkReleases.get().getReleases()) {
        JdkVersion v = r.getVersion();
        System.out.println("  " + v.longBuild() + " / " + v.shortBuild() + (r.isPsu() ? " PSU" : ""));
      }
      return;
    }


    String v = cli.getOptionValue("v");//"1.8.0_92-b14";
    String o = cli.getOptionValue("o");
    String a = cli.getOptionValue("a");

    if (cli.hasOption('?')) {
      usage();
      return;
    }

    if (o == null) {
      System.err.println("No output dir specified");
      usage();
      return;
    }

    if (v == null) {
      System.err.println("No version specified");
      usage();
      return;
    }

    Arch arch = null;
    if (a != null) {
      arch = parseArch(a);
      if (arch == null) {
        usage();
        return;
      }
    }

    File jdkDir = new File(o);
    JdkGetter getter = JdkGetter.builder()
      .version(v)
      .outputDirectory(jdkDir)
      .arch(arch)
      .build();

    getter.get();
  }

  private static Arch parseArch(String a) {

    a = simpleArch(a);
    for (Arch arch : Arch.values()) {
      String av = simpleArch(arch.name());
      if (a.equals(av)) {
        return arch;
      }
    }
    return null;
  }

  private static String simpleArch(String a) {
    return a.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private static void usage() {
    System.out.println("Usage:");
    System.out.println("List versions: java -jar jdkget.jar -l");
    System.out.println("Download and extract: java -jar jdkget.jar -o <outputDir> -v <jdkVersion> [-a <arch>]");
    System.out.println("  Version format: 1.<major>.0_<rev>-<build> (Ex. 1.8.0_92-b14)");
    System.out.println("  Available architectures:");
    for (Arch ar : Arch.values()) {
      System.out.println("    " + simpleArch(ar.name()));
    }
  }


}
