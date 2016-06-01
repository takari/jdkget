package io.takari.jdkget;

import java.io.File;

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

  public JdkGetter(String version, Arch arch, File outputDirectory, ITransport transport) {
    this.jdkVersion = JdkVersion.parse(version);
    if(arch != null) {
      this.arch = arch;
    } else {
      this.arch = Arch.autodetect();
      System.out.println("Autodetected arch: " + this.arch);
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

  public void get() throws Exception {
    if (!inProcessDirectory.exists()) {
      inProcessDirectory.mkdirs();
    }
    if (!jdkImage.exists()) {
      transport.downloadJdk(arch, jdkVersion, jdkImage);
    } else {
      System.out.println("We already have a copy of " + jdkImage);
    }
    
    getExtractor(arch).extractJdk(jdkVersion, jdkImage, outputDirectory, inProcessDirectory);
    
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
    private ITransport transport;
    private File outputDirectory;

    public JdkGetter build() {
      return new JdkGetter(version, arch, outputDirectory, transport);
    }
    
    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder arch(Arch arch) {
      this.arch = arch;
      return this;
    }

    public Builder transport(ITransport transport) {
      this.transport = transport;
      return this;
    }

    public Builder outputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }
  }

  public static void main(String[] args) throws Exception {
    String version = "1.8.0_92-b14";
    File jdkDir = new File("./jdk-8u92");
    JdkGetter getter = JdkGetter.builder()
      .version(version)
      .outputDirectory(jdkDir)
      .build();

    getter.get();
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
}
