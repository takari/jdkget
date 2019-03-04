package io.takari.jdkget.model;

import java.io.Serializable;

import io.takari.jdkget.Arch;

public class JdkBinary implements Serializable {
  private static final long serialVersionUID = 1L;

  private JdkRelease release;
  private final Arch arch;
  private final String descriptor; // combination of original architecture name and extension
  private final String path;
  private final String md5;
  private final String sha256;
  private final long size;

  JdkBinary(Arch arch, String descriptor, String path, String md5, String sha256, long size) {
    this.arch = arch;
    this.descriptor = descriptor;
    this.path = path;
    this.md5 = md5;
    this.sha256 = sha256;
    this.size = size;
  }

  public JdkRelease getRelease() {
    return release;
  }

  void setRelease(JdkRelease release) {
    if (this.release != null) {
      throw new IllegalStateException("Release already set");
    }
    this.release = release;
  }

  public Arch getArch() {
    return arch;
  }

  public String getDescriptor() {
    return descriptor;
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
