package io.takari.jdkget;

import static org.junit.Assert.assertEquals;

import io.takari.jdkget.model.JdkVersion;

import org.junit.Test;

public class VersionsTest {

  @Test
  public void testShortestVersion() {
    JdkVersion v = JdkVersion.parse("8");
    assertEquals(8, v.major);
    assertEquals(-1, v.minor);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortestVersionUpdate() {
    JdkVersion v = JdkVersion.parse("8u92");
    assertEquals(8, v.major);
    assertEquals(92, v.minor);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortestVersionUpdateBuild() {
    JdkVersion v = JdkVersion.parse("8u92-b14");
    assertEquals(8, v.major);
    assertEquals(92, v.minor);
    assertEquals("-b14", v.buildNumber);
  }

  @Test
  public void testShortVersion() {
    JdkVersion v = JdkVersion.parse("1.8");
    assertEquals(8, v.major);
    assertEquals(-1, v.minor);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortVersionUpdate() {
    JdkVersion v = JdkVersion.parse("1.8_92");
    assertEquals(8, v.major);
    assertEquals(92, v.minor);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortVersionUpdateBuild() {
    JdkVersion v = JdkVersion.parse("1.8_92-b14");
    assertEquals(8, v.major);
    assertEquals(92, v.minor);
    assertEquals("-b14", v.buildNumber);
  }

  @Test
  public void test9() {
    JdkVersion v = JdkVersion.parse("9+181");
    assertEquals(9, v.major);
    assertEquals(0, v.minor);
    assertEquals(0, v.security);
    assertEquals("+181", v.buildNumber);

    JdkVersion v2 = JdkVersion.parse("9");
    assertEquals(9, v2.major);
    assertEquals(-1, v2.minor);
    assertEquals(-1, v2.security);
    assertEquals(null, v2.buildNumber);

    JdkVersion v3 = JdkVersion.parse("9.0.1");
    assertEquals(9, v3.major);
    assertEquals(0, v3.minor);
    assertEquals(1, v3.security);
    assertEquals(null, v3.buildNumber);
    assertEquals("9.0.1", v3.longVersion());

    JdkVersion v4 = JdkVersion.parse("9.1.1");
    assertEquals(9, v4.major);
    assertEquals(1, v4.minor);
    assertEquals(1, v4.security);
    assertEquals(null, v4.buildNumber);

    JdkVersion v5 = JdkVersion.parse("9.1");
    assertEquals(9, v5.major);
    assertEquals(1, v5.minor);
    assertEquals(-1, v5.security);
    assertEquals(null, v5.buildNumber);

    JdkVersion v6 = JdkVersion.parse("9.");
    assertEquals(9, v6.major);
    assertEquals(0, v6.minor);
    assertEquals(0, v6.security);
    assertEquals(null, v6.buildNumber);

  }
}
