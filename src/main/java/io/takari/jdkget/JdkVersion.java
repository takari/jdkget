package io.takari.jdkget;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

public class JdkVersion implements Comparable<JdkVersion>, Serializable {
  private static final long serialVersionUID = 1L;

  public final int major;

  public final int revision;

  public final String buildNumber;

  private JdkVersion(int major, int revision, String buildNumber) {
    this.major = major;
    this.revision = revision;
    this.buildNumber = buildNumber;
  }

  public String toString() {
    return longBuild();
  }

  public String longVersion() {
    StringBuilder sb = new StringBuilder();
    sb.append("1.").append(major).append(".0");
    if (revision > 0)
      sb.append('_').append(revision);
    return sb.toString();
  }

  public String longBuild() {
    StringBuilder sb = new StringBuilder();
    sb.append("1.").append(major).append(".0");
    if (revision > 0)
      sb.append('_').append(revision);
    sb.append(buildNumber);
    return sb.toString();
  }

  public String shortVersion() {
    StringBuilder sb = new StringBuilder();
    sb.append(major);
    if (revision > 0)
      sb.append('u').append(revision);
    return sb.toString();
  }

  public String shortBuild() {
    StringBuilder sb = new StringBuilder();
    sb.append(major);
    if (revision > 0)
      sb.append('u').append(revision);
    sb.append(buildNumber);
    return sb.toString();
  }

  public static JdkVersion parse(String version) {
    if (version.startsWith("1.")) { // 1.8.0_91-b14
      String[] p = StringUtils.split(version, "_");
      String major = StringUtils.split(p[0], ".")[1];

      int revision = -1;
      String buildNumber = "";
      if (p.length > 1) {
        String revisionWithBuildNumber = p[1];
        String[] x = StringUtils.split(revisionWithBuildNumber, "-");
        revision = i(x[0]);
        if (x.length > 1) {
          buildNumber = "-" + x[1];
        }
      }
      return new JdkVersion(i(major), revision, buildNumber);
    }

    if (version.contains("u")) { // 8u91-b14
      String[] p = StringUtils.split(version, "u");
      String major = p[0];

      String revisionWithBuildNumber = p[1];

      String[] x = StringUtils.split(revisionWithBuildNumber, "-");
      String revision = x[0];

      String buildNumber;
      if (revision.endsWith("b")) { // 6u5b
        buildNumber = revision.substring(revision.length() - 1);
        revision = revision.substring(0, revision.length() - 1);
      } else {
        buildNumber = x.length > 1 ? "-" + x[1] : "";
      }
      return new JdkVersion(i(major), i(revision), buildNumber);
    }

    if (version.contains("-")) { //8-b132
      String[] x = StringUtils.split(version, "-");
      String major = x[0];
      String buildNumber = x.length > 1 ? "-" + x[1] : "";
      return new JdkVersion(i(major), -1, buildNumber);
    }

    return new JdkVersion(i(version), -1, ""); // 7

    //throw new IllegalArgumentException("Unsupported version format: " + version);
  }

  private static int i(String s) {
    return s == null ? -1 : Integer.parseInt(s);
  }

  int buildNum() {
    String b = buildNumber;
    if (b.startsWith("-")) {
      b = b.substring(1);
    }
    if (b.startsWith("b")) {
      b = b.substring(1);
    }
    if (b.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(b);
  }

  @Override
  public int compareTo(JdkVersion o) {
    int c = major - o.major;
    if (c == 0) {
      c = revision - o.revision;
    }
    if (c == 0) {
      c = buildNum() - o.buildNum();
    }
    return c;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JdkVersion) {
      JdkVersion that = (JdkVersion) obj;
      return major == that.major && revision == that.revision;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return major * 19 + revision;
  }

}