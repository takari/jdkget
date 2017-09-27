package io.takari.jdkget;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

public abstract class JdkVersion implements Comparable<JdkVersion>, Serializable {
  private static final long serialVersionUID = 1L;

  public final int major;
  public final int minor;
  public final int security;
  public final String buildNumber;

  protected JdkVersion(int major, int minor, int security, String buildNumber) {
    this.major = major;
    this.minor = minor;
    this.security = security;
    this.buildNumber = buildNumber;
  }

  public abstract String shortBuild();

  public abstract String shortVersion();

  public abstract String longBuild();

  public abstract String longVersion();

  public String toString() {
    return longBuild();
  }

  public static JdkVersion parse(String version) {
    {
      // 9+build
      // 9.<maj>.<min>+build
      // 9.<maj>.<min>
      // 9.<maj>
      // 9
      int majEnd = findDigits(version, 0);
      int major = i(version.substring(0, majEnd));
      if (major >= 9) {
        int minor = -1;
        int security = -1;
        String build = null;

        if (version.length() > majEnd && version.charAt(majEnd) == '.') {
          int minEnd = findDigits(version, majEnd + 1);
          minor = i(version.substring(majEnd + 1, minEnd));
          if(minor == -1) {
            // 9. -> 9.0.0 selected
            minor = 0;
            security = 0;
          }

          if (security == -1 && version.length() > minEnd && version.charAt(minEnd) == '.') {
            int secEnd = findDigits(version, minEnd + 1);
            security = i(version.substring(minEnd + 1, secEnd));
          }
        }

        int plus = version.indexOf('+');
        if (plus != -1) {
          build = version.substring(plus);
          if (minor == -1) {
            // concrete version selected
            minor = 0;
            security = 0;
          }
        }
        return new JdkVersionPost9(major, minor, security, build);
      }
    }

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
      return new JdkVersionPre9(i(major), revision, buildNumber);
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
      return new JdkVersionPre9(i(major), i(revision), buildNumber);
    }

    if (version.contains("-")) { // 8-b132
      String[] x = StringUtils.split(version, "-");
      String major = x[0];
      String buildNumber = x.length > 1 ? "-" + x[1] : "";
      return new JdkVersionPre9(i(major), -1, buildNumber);
    }

    return new JdkVersionPre9(i(version), -1, ""); // 7

    // throw new IllegalArgumentException("Unsupported version format: " + version);
  }

  private static int findDigits(String version, int start) {
    int end = start;
    while (end < version.length()) {
      if (!Character.isDigit(version.charAt(end))) {
        break;
      }
      end++;
    }
    return end;
  }

  private static int i(String s) {
    return s == null || s.length() == 0 ? -1 : Integer.parseInt(s);
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
      c = minor - o.minor;
    }
    if (c == 0) {
      c = security - o.security;
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
      return major == that.major && minor == that.minor && security == that.security;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return major * 19 + minor;
  }

  public static class JdkVersionPre9 extends JdkVersion {
    private static final long serialVersionUID = 1L;

    protected JdkVersionPre9(int major, int revision, String buildNumber) {
      super(major, revision, -1, buildNumber);
    }

    @Override
    public String longVersion() {
      StringBuilder sb = new StringBuilder();
      sb.append("1.").append(major).append(".0");
      if (minor > 0)
        sb.append('_').append(minor);
      return sb.toString();
    }

    @Override
    public String longBuild() {
      StringBuilder sb = new StringBuilder();
      sb.append("1.").append(major).append(".0");
      if (minor > 0)
        sb.append('_').append(minor);
      sb.append(buildNumber);
      return sb.toString();
    }

    @Override
    public String shortVersion() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (minor > 0)
        sb.append('u').append(minor);
      return sb.toString();
    }

    @Override
    public String shortBuild() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (minor > 0)
        sb.append('u').append(minor);
      sb.append(buildNumber);
      return sb.toString();
    }
  }

  public static class JdkVersionPost9 extends JdkVersion {
    private static final long serialVersionUID = 1L;

    protected JdkVersionPost9(int major, int minor, int security, String buildNumber) {
      super(major, minor, security, buildNumber);
    }

    @Override
    public String longVersion() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (minor > 0)
        sb.append('.').append(minor);
      if (minor > 0)
        sb.append('.').append(security);
      return sb.toString();
    }

    @Override
    public String longBuild() {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      if (minor > 0)
        sb.append('.').append(minor);
      if (minor > 0)
        sb.append('.').append(security);
      sb.append(buildNumber);
      return sb.toString();
    }

    @Override
    public String shortVersion() {
      return longVersion();
    }

    @Override
    public String shortBuild() {
      return longBuild();
    }

  }

}
