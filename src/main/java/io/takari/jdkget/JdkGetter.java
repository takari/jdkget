package io.takari.jdkget;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ext.Java7SupportImpl;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import io.takari.jdkget.JdkReleases.JCE;
import io.takari.jdkget.JdkReleases.JavaReleaseType;
import io.takari.jdkget.JdkReleases.JdkBinary;
import io.takari.jdkget.JdkReleases.JdkRelease;
import io.takari.jdkget.extract.BinJDKExtractor;
import io.takari.jdkget.extract.OsxJDKExtractor;
import io.takari.jdkget.extract.TZJDKExtractor;
import io.takari.jdkget.extract.TgzJDKExtractor;
import io.takari.jdkget.extract.WindowsJDKExtractor;

public class JdkGetter {

  private final JdkVersion jdkVersion;
  private final boolean unrestrictedJCE;
  private final Arch arch;
  private final String type;
  private final File outputDirectory;
  private final File inProcessDirectory;
  private final int retries;
  private ITransport transport;
  private IOutput output;
  private boolean silent;

  private JdkGetter(String version, Arch arch, String type, File outputDirectory, int retries, ITransport transport, IOutput output, boolean silent) {
    this(version, false, arch, type, outputDirectory, retries, transport, output, silent);
  }

  private JdkGetter(String version, boolean unrestrictedJCE, Arch arch, String type, File outputDirectory, int retries, ITransport transport, IOutput output, boolean silent) {
    this(version == null || version.equals("latest") ? null : JdkVersion.parse(version), unrestrictedJCE, arch, type, outputDirectory, retries, transport, output, silent);
  }

  private JdkGetter(JdkVersion jdkVersion, boolean unrestrictedJCE, Arch arch, String type, File outputDirectory, int retries, ITransport transport, IOutput output, boolean silent) {
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

    this.type = JavaReleaseType.validateTypeName(type);
    this.silent = silent;

    this.outputDirectory = outputDirectory.getAbsoluteFile();
    this.inProcessDirectory = new File(outputDirectory.getPath() + ".in-process");
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

    JdkContext context = new JdkContext(theVersion, arch, type, output);
    context.setSilent(silent);

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
        output.info("Ignoring an error building JSA cache:");
        for (String l : stdout) {
          output.info(" > " + l);
        }
      }
    } catch (Throwable t) {
      Throwables.propagateIfInstanceOf(t, InterruptedException.class);
      output.info("Ignoring an error building JSA cache: " + t);
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
    private JdkVersion jdkVersion;
    private boolean unrestrictedJCE;
    private Arch arch;
    private File outputDirectory;
    private int retries = 0;
    private ITransport transport;
    private IOutput output;
    private String type;
    private boolean silent;

    public JdkGetter build() {
      return new JdkGetter(jdkVersion, unrestrictedJCE, arch, type, outputDirectory, retries, transport, output, silent);
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

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder silent() {
      this.silent = true;
      return this;
    }

  }

  private static final Options cliOptions = new Options();
  static {
    cliOptions.addOption("o", true, "Output dir");
    cliOptions.addOption("v", true, "JDK Version");
    cliOptions.addOption("t", true, "Java release type of: jdk(default), jre, serverjre");
    cliOptions.addOption("a", true, "Architecture");
    cliOptions.addOption("l", false, "List versions");
    cliOptions.addOption("u", true, "Alternate url to oracle.com/otn-pub");
    cliOptions.addOption("jce", false, "Also install unlimited jce policy");
    cliOptions.addOption("otnUser", true, "OTN username");
    cliOptions.addOption("otnPassword", true, "OTN password");
    cliOptions.addOption("mirror", false, "Mirror remote storage by only downloading binaries; can be used with -v, -vf, -vt, -t and -a, otherwise will download everything");
    cliOptions.addOption("threads", true, "Number of threads to run mirror with");
    cliOptions.addOption("vf", true, "When used with -mirror, specifies version range 'from'");
    cliOptions.addOption("vt", true, "When used with -mirror, specifies version range 'to'");
    cliOptions.addOption("s", false, "Silence download messages");
    cliOptions.addOption("?", "help", false, "Help");
  }

  public static void main(String[] args) throws Exception {
    Preconditions.checkNotNull(Java7SupportImpl.class); // prime it to prevent errors later

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
    String v = cli.getOptionValue("v");// "1.8.0_92-b14";
    String[] a = cli.getOptionValues("a");
    String[] t = cli.getOptionValues("t");
    boolean silent = cli.hasOption("s");

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
    Set<Arch> arches = parseArches(a);

    ITransport transport = null;
    if (u != null) {
      transport = new OracleWebsiteTransport(u, otnu, otnp);
    } else {
      transport = new OracleWebsiteTransport(OracleWebsiteTransport.ORACLE_WEBSITE, otnu, otnp);
    }

    if (mirror) {
      String vf = cli.getOptionValue("vf");
      String vt = cli.getOptionValue("vt");

      int threads = 1;
      if (cli.hasOption("threads")) {
        threads = Integer.parseInt(cli.getOptionValue("threads"));
      }

      mirrorRemote(transport, v != null ? v : vf, v != null ? v : vt, arches, t, outDir, threads, silent);
      return;
    }

    if (v == null) {
      System.err.println("No version specified");
      usage();
      return;
    }

    Arch arch = null;
    if (!arches.isEmpty()) {
      if (arches.size() == 1) {
        arch = arches.iterator().next();
      } else {
        System.err.println("Only one arch is allowed");
      }
    }
    if (arch == null) {
      usage();
      return;
    }

    if (t != null && t.length != 1) {
      System.err.println("Only one type is allowed: " + Arrays.toString(t));
      usage();
      return;
    }

    if (t != null && StringUtils.equals("n/a", JavaReleaseType.validateTypeName(t[0], "n/a"))) {
      System.err.println("Release type is not supported: " + t[0]);
      System.err.print("Avalable release types: " +
          Arrays.asList(JavaReleaseType.values()).stream().map(at -> at.getName())
              .collect(Collectors.joining(", ")));
      usage();
      return;
    }

    JdkGetter.Builder b = JdkGetter.builder() //
        .type(t == null ? null : t[0]) //
        .version(v) //
        .outputDirectory(outDir) //
        .arch(arch) //
        .transport(transport);

    if (jce) {
      b = b.unrestrictedJCE();
    }

    if (silent) {
      b = b.silent();
    }

    b.build().get();
  }

  private static void mirrorRemote(ITransport transport, String vfrom, String vto, Set<Arch> arch, String[] types, File outDir, int threads, boolean silent) throws IOException, InterruptedException {
    JdkReleases rels = JdkReleases.get();
    JdkVersion vf = vfrom != null ? JdkVersion.parse(vfrom) : null;
    JdkVersion vt = vto != null ? rels.select(JdkVersion.parse(vto)).getVersion() : null;

    for (String v : Arrays.asList("8", "7", "6")) {
      JdkVersion version = JdkVersion.parse(v);
      JCE jce = rels.getJCE(version);
      File jceFile = new File(outDir, jce.getPath());
      if (!jceFile.exists()) {
        CachingOutput output = new CachingOutput();
        jceFile.getParentFile().mkdirs();
        JdkContext ctx = new JdkContext(rels, version, null, null, output);
        ctx.setSilent(true);
        output.info("** Downloading jce policy files to " + jceFile);
        transport.downloadJce(ctx, jceFile);
        output.output(System.out);
      }
    }

    ExecutorService ex = Executors.newFixedThreadPool(threads);
    for (JdkRelease rel : rels.getReleases()) {
      JdkVersion v = rel.getVersion();
      if (vt != null && vt.compareTo(v) < 0) {
        continue;
      }
      if (vf != null && v.compareTo(vf) < 0) {
        break;
      }

      Collection<String> binTypes = rel.getTypes(JavaReleaseType.validateTypeNames(types));
      if (binTypes == null) {
        continue;
      }

      for (String t : binTypes) {
        Set<Arch> reqArches = EnumSet.copyOf(arch);
        if (reqArches.isEmpty()) {
          reqArches.addAll(rel.getArchs(t));
        } else {
          reqArches.retainAll(rel.getArchs(t));
        }
        if (reqArches.isEmpty()) {
          continue;
        }
        mirrorRemoteDownloading(transport, outDir, rels, rel, v, reqArches, t, silent || threads > 1, ex);
      }
    }
    ex.shutdown();
    ex.awaitTermination(7, TimeUnit.DAYS);
  }

  private static void mirrorRemoteDownloading(ITransport transport, File outDir, JdkReleases rels,
      JdkRelease rel, JdkVersion v, Collection<Arch> arches, String type, boolean silent, ExecutorService ex)
      throws IOException, InterruptedException {
    for (Arch a : arches) {
      for(JdkBinary bin: rel.getBinaries(type, a)) {

        ex.submit(() -> {
          CachingOutput output = new CachingOutput();
          JdkContext ctx = new JdkContext(rels, v, a, type, output);
          ctx.setSilent(silent);
          ctx.setBinDescriptor(bin.getDescriptor());
          try {
            File out = new File(outDir, bin.getPath()).getAbsoluteFile();
            if (out.exists()) {
              output.info("** Checking " + type + "-" + v.shortBuild() + " (" + a.name() + ") in " + out);
              if (transport.validate(ctx, out)) {
                return;
              } else {
                ctx.getOutput().info("Existing file failed validation, deleting");
                FileUtils.forceDelete(out);
              }
            }
            output.info("** Downloading " + type + "-" + v.shortBuild() + " (" + a.name() + ") to " + out);
            FileUtils.forceMkdir(out.getParentFile());
            transport.downloadJdk(ctx, out);
            if (!transport.validate(ctx, out)) {
              ctx.getOutput().error("Invalid image file " + out);
            }
          } catch (Exception e) {
            output.error("Error downloading", e);
          } finally {
            output.output(System.out);
          }
        });

      }
    }
  }

  private static Set<Arch> parseArches(String[] as) {
    Set<Arch> arches = EnumSet.noneOf(Arch.class);
    if (as != null) {
      for (String a : as) {
        a = simpleArch(a);
        for (Arch arch : Arch.values()) {
          String av = simpleArch(arch.name());
          if (a.equals(av)) {
            arches.add(arch);
          }
        }
      }
    }
    return arches;
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
    System.out.println("    jdkget-" + ver + ".jar -o <outputDir> -v <jdkVersion> [-t <type>] [-a <arch>]");
    System.out.println("  Mirror remote:");
    System.out.println("    jdkget-" + ver + ".jar -mirror -o <outputDir> [-t <type1> ... -t <typeN>] [-v <jdkVersion>] [-vf <fromVersion>] [-vt <toVersion>] [-a <arch>]");
  }


}
