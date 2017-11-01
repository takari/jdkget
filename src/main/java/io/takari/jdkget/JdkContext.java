package io.takari.jdkget;

public class JdkContext {
  private JdkReleases releases;
  private JdkVersion version;
  private Arch arch;
  private String type;
  private IOutput output;

  public JdkContext(JdkReleases releases, JdkVersion version, Arch arch, String type, IOutput output) {
    this.releases = releases;
    this.version = version;
    this.arch = arch;
    this.type = type;
    this.output = output;
  }

  public JdkReleases getReleases() {
    return releases;
  }

  public JdkVersion getVersion() {
    return version;
  }

  public Arch getArch() {
    return arch;
  }

  public IOutput getOutput() {
    return output;
  }

  public String getType() {
    return type;
  }
}
