package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;

import com.google.common.base.Throwables;

import io.takari.jdkget.JdkReleases.JdkRelease;
import io.takari.jdkget.extract.BinJDKExtractor;
import io.takari.jdkget.extract.OsxJDKExtractor;
import io.takari.jdkget.extract.TZJDKExtractor;
import io.takari.jdkget.extract.TgzJDKExtractor;
import io.takari.jdkget.extract.WindowsJDKExtractor;

public class JdkGetter {

  private JdkReleases releases;
  private final JdkVersion jdkVersion;
  private final boolean unrestrictedJCE;
  private final Arch arch;
  private final File outputDirectory;
  private final File inProcessDirectory;
  private final int retries;
  private ITransport transport;
  private IOutput output;

  public JdkGetter(JdkReleases releases, String version, Arch arch, File outputDirectory, int retries, ITransport transport, IOutput output) {
    this(releases, version, false, arch, outputDirectory, retries, transport, output);
  }

  public JdkGetter(JdkReleases releases, String version, boolean unrestrictedJCE, Arch arch, File outputDirectory, int retries, ITransport transport, IOutput output) {
    this(releases, version == null || version.equals("latest") ? null : JdkVersion.parse(version), unrestrictedJCE, arch, outputDirectory, retries, transport, output);
  }

  public JdkGetter(JdkReleases releases, JdkVersion jdkVersion, boolean unrestrictedJCE, Arch arch, File outputDirectory, int retries, ITransport transport, IOutput output) {
    this.releases = releases;
    this.jdkVersion = jdkVersion;
    this.unrestrictedJCE = unrestrictedJCE;
    this.retries = retries;
    if (output != null) {
      this.output = output;
    } else {
      this.output = StdOutput.INSTANCE;
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

  protected JdkReleases getReleases() throws IOException {
    if (releases == null) {
      releases = JdkReleases.get(output);
    }
    return releases;
  }

  public Arch getArch() {
    return arch;
  }

  public JdkVersion getJdkVersion() {
    return jdkVersion;
  }

  public void get() throws IOException, InterruptedException {
    if (!inProcessDirectory.exists()) {
      inProcessDirectory.mkdirs();
    }

    JdkVersion theVersion;
    if (jdkVersion != null) {
      theVersion = getReleases().select(jdkVersion).getVersion();
    } else {
      theVersion = getReleases().latest().getVersion();
    }

    output.info("Getting jdk " + theVersion.shortBuild() + " for " + arch.toString().toLowerCase().replace("_", ""));

    JdkContext context = new JdkContext(getReleases(), theVersion, arch, output);

    File jdkImage = transport.getImageFile(context, inProcessDirectory);
    File jceImage = null;
    if (unrestrictedJCE) {
      jceImage = new File(jdkImage.getParentFile(), jdkImage.getName() + "-jce.zip");
    }

    boolean valid = false;
    int retr = retries;

    while (!valid) {
      boolean dontRetry = retr <= 0;
      try {
        if (jdkImage.exists()) {
          if (transport.validate(context, jdkImage)) {
            output.info("We already have a valid copy of " + jdkImage);
          } else {
            output.info("Found existing invalid image");
            FileUtils.forceDelete(jdkImage);
          }
        }

        if (!jdkImage.exists()) {
          transport.downloadJdk(context, jdkImage);
        }

        if (!jdkImage.exists()) {
          output.error("Cannot download jdk " + theVersion.shortBuild() + " for " + arch);
          throw new IOException("Transport failed to download jdk image");
        }

        output.info("Validating downloaded image");

        Util.checkInterrupt();
        valid = transport.validate(context, jdkImage);
      } catch (InterruptedException e) {
        throw e;
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
    if (!extractor.extractJdk(context, jdkImage, outputDirectory, inProcessDirectory)) {
      throw new IOException("Failed to extract JDK from " + jdkImage);
    }

    if (jceImage != null) {
      transport.downloadJce(context, jceImage);
      new JCEExtractor().extractJCE(context, jceImage, outputDirectory, inProcessDirectory);
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
    private JdkReleases releases;
    private JdkVersion jdkVersion;
    private boolean unrestrictedJCE;
    private String version;
    private Arch arch;
    private File outputDirectory;
    private int retries = 0;
    private ITransport transport;
    private IOutput output;

    public JdkGetter build() {
      if (jdkVersion != null) {
        return new JdkGetter(releases, jdkVersion, unrestrictedJCE, arch, outputDirectory, retries, transport, output);
      }
      return new JdkGetter(releases, version, unrestrictedJCE, arch, outputDirectory, retries, transport, output);
    }

    public Builder releases(JdkReleases releases) {
      this.releases = releases;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder version(JdkVersion version) {
      this.jdkVersion = version;
      return this;
    }

    public Builder unrestrictedJCE() {
      this.unrestrictedJCE = true;
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

  public static void main(String[] args) throws Exception {

    Options options = new Options();
    options.addOption("o", true, "Output dir");
    options.addOption("v", true, "JDK Version");
    options.addOption("a", true, "Architecture");
    options.addOption("l", false, "List versions");
    options.addOption("u", true, "Alternate url to oracle.com/otn-pub");
    options.addOption("jce", false, "Also configure unlimited jce policy");
    options.addOption("otnUser", true, "OTN username");
    options.addOption("otnPassword", true, "OTN password");
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
    String u = cli.getOptionValue("u");
    boolean jce = cli.hasOption("jce");
    String otnu = cli.getOptionValue("otnUser");
    String otnp = cli.getOptionValue("otnPassword");

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
    JdkGetter.Builder b = JdkGetter.builder()
      .version(v)
      .outputDirectory(jdkDir)
      .arch(arch);

    if (u != null) {
      b = b.transport(new OracleWebsiteTransport(u, otnu, otnp));
    } else if(otnu != null || otnp != null) {
      b = b.transport(new OracleWebsiteTransport(OracleWebsiteTransport.ORACLE_WEBSITE, otnu, otnp));
    }

    if(jce) {
      b = b.unrestrictedJCE();
    }

    b.build().get();
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
