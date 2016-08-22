package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import io.takari.jdkget.osx.OsxJDKExtractor;
import io.takari.jdkget.transport.OracleWebsiteTransport;
import io.takari.jdkget.win32.WindowsJDKExtractor;

public class JdkGetter {

  private final JdkVersion jdkVersion;
  private final Arch arch;
  private final File outputDirectory;
  private final File inProcessDirectory;
  private final File jdkImage;
  private ITransport transport;
  private IOutput output;
  
  public JdkGetter(String version, Arch arch, File outputDirectory, ITransport transport, IOutput output) {
    
    if(output != null) {
      this.output = output;
    } else {
      this.output = new StdOutput();
    }
    
    this.jdkVersion = JdkVersion.parse(version);
    if(arch != null) {
      this.arch = arch;
    } else {
      this.arch = Arch.autodetect();
      this.output.info("Autodetected arch: " + this.arch);
    }
    if(transport != null) {
      this.transport = transport;
    } else {
      this.transport = new OracleWebsiteTransport();
    }
    this.outputDirectory = outputDirectory.getAbsoluteFile();
    this.inProcessDirectory = new File(outputDirectory.getPath() + ".in-process");
    this.jdkImage = new File(inProcessDirectory, String.format("jdk-%su%s-%s.%s", jdkVersion.major, jdkVersion.revision, 
        this.arch.getArch(), this.arch.getExtension()));
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
    if (!jdkImage.exists()) {
      if(!transport.downloadJdk(arch, jdkVersion, jdkImage, output)) {
        output.error("Could not get jdk image");
        return;
      }
    } else {
      output.info("We already have a copy of " + jdkImage);
    }
    
    if (!getExtractor(arch).extractJdk(jdkVersion, jdkImage, outputDirectory, inProcessDirectory, output)) {
      throw new IOException("Failed to extract JDK from " + jdkImage);
    }

    FileUtils.deleteDirectory(inProcessDirectory);
  }
  
  private static IJdkExtractor getExtractor(Arch arch) {
    
    switch(arch) {
    case OSX_64:
      return new OsxJDKExtractor();
    case WIN_32:
    case WIN_64:
      return new WindowsJDKExtractor();
    case NIX_32:
    case NIX_64:
    case SOL_64:
    case SOL_SPARC:
      return new TgzJDKExtractor();
    }
    
    throw new IllegalArgumentException("Unsupported arch: " + arch);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String version;
    private Arch arch;
    private File outputDirectory;
    private ITransport transport;
    private IOutput output;

    public JdkGetter build() {
      return new JdkGetter(version, arch, outputDirectory, transport, output);
    }
    
    public Builder version(String version) {
      this.version = version;
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
    
    public Builder transport(ITransport transport) {
      this.transport = transport;
      return this;
    }

    public Builder output(IOutput output) {
      this.output = output;
      return this;
    }
  }

  public static class JdkVersion {
    
    public final String major;
    
    public final String revision;
    
    public final String buildNumber;

    private JdkVersion(String major, String revision, String buildNumber) {
      this.major = major;
      this.revision = revision;
      this.buildNumber = buildNumber;
    }
    
    public String toString() {
      return String.format("1.%s.0_%s-%s", major, revision, buildNumber);
    }
    
    public static JdkVersion parse(String version) {
      String[] p = StringUtils.split(version, "_");
      String major = StringUtils.split(p[0], ".")[1];
      String revisionWithBuildNumber = p[1];
      String[] x = StringUtils.split(revisionWithBuildNumber, "-");
      String revision = x[0];
      String buildNumber = x[1];
      
      return new JdkVersion(major, revision, buildNumber);
    }
    
  }
  
  public static void main(String[] args) throws Exception {
    
    Options options = new Options();
    options.addOption("o", true, "Output dir");
    options.addOption("v", true, "JDK Version");
    options.addOption("a", true, "Architecture");
    options.addOption("?", "help", false, "Help");
    
    CommandLine cli = new PosixParser().parse(options, args);
    
    String v = cli.getOptionValue("v");//"1.8.0_92-b14";
    String o = cli.getOptionValue("o");
    String a = cli.getOptionValue("a");
    
    if(cli.hasOption('?')) {
      usage();
      return;
    }
    
    if(o == null) {
      System.err.println("No output dir specified");
      usage();
      return;
    }
    
    if(v == null) {
      System.err.println("No version specified");
      usage();
      return;
    }
    
    Arch arch = null;
    if(a != null) {
      arch = parseArch(a);
      if(arch == null) {
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
    for(Arch arch: Arch.values()) {
      String av = simpleArch(arch.name());
      if(a.equals(av)) {
        return arch;
      }
    }
    return null;
  }

  private static String simpleArch(String a) {
    return a.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private static void usage() {
    System.out.println("Usage: java -jar jdkget.jar -o <outputDir> -v <jdkVersion> [-a <arch>]");
    System.out.println("  Version format: 1.<major>.0_<rev>-<build> (Ex. 1.8.0_92-b14)");
    System.out.println("  Available architectures:");
    for(Arch ar: Arch.values()) {
      System.out.println("    " + simpleArch(ar.name()));
    }
  }
  

}
