package io.takari.jdkget;

public class JdkContext {
  private JdkVersion version;
  private Arch arch;
  private String type;
  private IOutput output;

  private boolean silent;
  private String binDescriptor;

  public JdkContext(JdkVersion version, Arch arch, String type, IOutput output) {
    this.version = version;
    this.arch = arch;
    this.type = type;
    this.output = output;
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

  public boolean isSilent() {
    return silent;
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public String getBinDescriptor() {
    return binDescriptor;
  }

  public void setBinDescriptor(String binDescriptor) {
    this.binDescriptor = binDescriptor;
  }
}
