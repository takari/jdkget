package io.takari.jdkget.model;

import java.io.Serializable;

public class JCE implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int majorVersion;
  private final String path;

  public JCE(int majorVersion, String path) {
    this.majorVersion = majorVersion;
    this.path = path;
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public String getPath() {
    return path;
  }

}