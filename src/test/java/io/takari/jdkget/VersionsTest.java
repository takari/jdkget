package io.takari.jdkget;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.takari.jdkget.JdkGetter.JdkVersion;

public class VersionsTest {

  @Test
  public void testShortestVersion() {
    JdkVersion v = JdkVersion.parse("8");
    assertEquals(8, v.major);
    assertEquals(-1, v.revision);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortestVersionUpdate() {
    JdkVersion v = JdkVersion.parse("8u92");
    assertEquals(8, v.major);
    assertEquals(92, v.revision);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortestVersionUpdateBuild() {
    JdkVersion v = JdkVersion.parse("8u92-b14");
    assertEquals(8, v.major);
    assertEquals(92, v.revision);
    assertEquals("-b14", v.buildNumber);
  }

  @Test
  public void testShortVersion() {
    JdkVersion v = JdkVersion.parse("1.8");
    assertEquals(8, v.major);
    assertEquals(-1, v.revision);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortVersionUpdate() {
    JdkVersion v = JdkVersion.parse("1.8_92");
    assertEquals(8, v.major);
    assertEquals(92, v.revision);
    assertEquals("", v.buildNumber);
  }

  @Test
  public void testShortVersionUpdateBuild() {
    JdkVersion v = JdkVersion.parse("1.8_92-b14");
    assertEquals(8, v.major);
    assertEquals(92, v.revision);
    assertEquals("-b14", v.buildNumber);
  }
}
