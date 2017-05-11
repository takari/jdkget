package io.takari.jdkget;

public class JdkContext {
  private JdkReleases releases;
  private JdkVersion version;
  private Arch arch;
  private IOutput output;

  public JdkContext(JdkReleases releases, JdkVersion version, Arch arch, IOutput output) {
    this.releases = releases;
    this.version = version;
    this.arch = arch;
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

}
