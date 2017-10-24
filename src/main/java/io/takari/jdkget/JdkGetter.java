package io.takari.jdkget;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.base.Throwables;

import io.takari.jdkget.JdkReleases.JdkBinary;
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
    boolean jceFix = false;
    if (unrestrictedJCE && theVersion.major < 9) {

      if (theVersion.major == 8 && theVersion.minor >= 151) {
        jceFix = true;
      } else {
        jceImage = new File(jdkImage.getParentFile(), jdkImage.getName() + "-jce.zip");
      }
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

    if (jceImage != null) {
      transport.downloadJce(context, jceImage);
      new JCEExtractor().extractJCE(context, jceImage, jdkHome, inProcessDirectory);
    }
    if (jceFix) {
      new JCEExtractor().fixJce(context, jdkHome);
    }

    // rebuild jsa cache (https://docs.oracle.com/javase/9/vm/class-data-sharing.htm)
    // but only if we're running on a compatible system (usually we do)
    if (arch == Arch.autodetect()) {
      rebuildJsa(jdkHome);
    }

    FileUtils.deleteDirectory(inProcessDirectory);
  }

  private void rebuildJsa(File jdkHome) throws IOException, InterruptedException {
    output.info("Building JSA cache");
    String cmd = new File(jdkHome, arch.isWindows() ? "bin\\java.exe" : "bin/java").getAbsolutePath();
    Process proc = new ProcessBuilder(cmd, "-Xshare:dump")
        .directory(jdkHome)
        .redirectErrorStream(true)
        .start();

    InputStream in = proc.getInputStream();
    List<String> stdout = IOUtils.readLines(in, Charset.defaultCharset());
    int ret = proc.waitFor();
    if (ret != 0) {
      for (String l : stdout) {
        output.error(l);
      }
    }
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

  private static final Options cliOptions = new Options();
  static {
    cliOptions.addOption("o", true, "Output dir");
    cliOptions.addOption("v", true, "JDK Version");
    cliOptions.addOption("a", true, "Architecture");
    cliOptions.addOption("l", false, "List versions");
    cliOptions.addOption("u", true, "Alternate url to oracle.com/otn-pub");
    cliOptions.addOption("jce", false, "Also install unlimited jce policy");
    cliOptions.addOption("otnUser", true, "OTN username");
    cliOptions.addOption("otnPassword", true, "OTN password");
    cliOptions.addOption("mirror", false, "Mirror remote storage by only downloading binaries; can be used with -v, -vf, -vt and -a, otherwise will download everything");
    cliOptions.addOption("vf", true, "When used with -mirror, specifies version range 'from'");
    cliOptions.addOption("vt", true, "When used with -mirror, specifies version range 'to'");
    cliOptions.addOption("?", "help", false, "Help");
  }

  public static void main(String[] args) throws Exception {

    CommandLine cli = new PosixParser().parse(cliOptions, args);

    if (cli.hasOption("l")) {
      System.out.println("Available JDK versions:");
      for (JdkRelease r : JdkReleases.get().getReleases()) {
        JdkVersion v = r.getVersion();
        System.out.println("  " + v.longBuild() + " / " + v.shortBuild() + (r.isPsu() ? " PSU" : ""));
      }
      return;
    }

    String o = cli.getOptionValue("o");

    String u = cli.getOptionValue("u");
    String otnu = cli.getOptionValue("otnUser");
    String otnp = cli.getOptionValue("otnPassword");

    boolean mirror = cli.hasOption("mirror");
    String vf = cli.getOptionValue("vf");
    String vt = cli.getOptionValue("vt");
    String v = cli.getOptionValue("v");// "1.8.0_92-b14";
    String a = cli.getOptionValue("a");

    boolean jce = cli.hasOption("jce");

    if (cli.hasOption('?')) {
      usage();
      return;
    }

    if (o == null) {
      System.err.println("No output dir specified");
      usage();
      return;
    }

    File outDir = new File(o);
    Arch arch = null;
    if (a != null) {
      arch = parseArch(a);
    }

    ITransport transport = null;
    if (u != null) {
      transport = new OracleWebsiteTransport(u, otnu, otnp);
    } else {
      transport = new OracleWebsiteTransport(OracleWebsiteTransport.ORACLE_WEBSITE, otnu, otnp);
    }

    if (mirror) {
      mirrorRemote(transport, v != null ? v : vf, v != null ? v : vt, arch, outDir);
      return;
    }

    if (v == null) {
      System.err.println("No version specified");
      usage();
      return;
    }

    if (arch == null) {
      usage();
      return;
    }

    JdkGetter.Builder b = JdkGetter.builder() //
        .version(v) //
        .outputDirectory(outDir) //
        .arch(arch) //
        .transport(transport);

    if (jce) {
      b = b.unrestrictedJCE();
    }

    b.build().get();
  }

  private static void mirrorRemote(ITransport transport, String vfrom, String vto, Arch arch, File outDir) throws IOException, InterruptedException {
    JdkReleases rels = JdkReleases.get();
    JdkVersion vf = vfrom != null ? JdkVersion.parse(vfrom) : null;
    JdkVersion vt = vto != null ? rels.select(JdkVersion.parse(vto)).getVersion() : null;

    for (JdkRelease rel : rels.getReleases()) {
      JdkVersion v = rel.getVersion();
      if (vt != null && vt.compareTo(v) < 0) {
        continue;
      }
      if (vf != null && v.compareTo(vf) < 0) {
        break;
      }

      Collection<Arch> arches = rel.getArchs();
      if (arch != null) {
        if (!arches.contains(arch)) {
          continue;
        }
        arches = Collections.singleton(arch);
      }
      for (Arch a : arches) {
        JdkBinary bin = rel.getBinary(a);
        File out = new File(outDir, bin.getPath()).getAbsoluteFile();
        FileUtils.forceMkdir(out.getParentFile());

        System.out.println("\n** Downloading " + v.shortBuild() + " for " + a.name() + " to " + out);
        JdkContext ctx = new JdkContext(rels, v, a, StdOutput.INSTANCE);
        if (out.exists()) {
          if (transport.validate(ctx, out)) {
            System.out.println("Valid file already exists");
            continue;
          } else {
            System.out.println("Existing file failed validation, deleting");
            FileUtils.forceDelete(out);
          }
        }
        transport.downloadJdk(ctx, out);
      }
    }
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
    String ver = JdkGetter.class.getPackage().getImplementationVersion();
    new HelpFormatter().printHelp("java -jar jdkget-" + ver + ".jar", cliOptions);
    System.out.println("\nVersion format: 1.<major>.0_<rev>-<build> or <major>u<rev>-<build> or <ver>+<build> (for 9+)");
    System.out.println("\nAvailable architectures:");
    for (Arch ar : Arch.values()) {
      System.out.println("    " + simpleArch(ar.name()));
    }

    System.out.println("\nExamples:");
    System.out.println("  List versions:");
    System.out.println("    jdkget-" + ver + ".jar -l");
    System.out.println("  Download and extract:");
    System.out.println("    jdkget-" + ver + ".jar -o <outputDir> -v <jdkVersion> [-a <arch>]");
    System.out.println("  Mirror remote:");
    System.out.println("    jdkget-" + ver + ".jar -mirror -o <outputDir> [-v <jdkVersion>] [-vf <fromVersion>] [-vt <toVersion>] [-a <arch>]");
  }


}
