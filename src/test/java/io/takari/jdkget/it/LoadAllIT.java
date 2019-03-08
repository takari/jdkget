package io.takari.jdkget.it;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import io.takari.jdkget.Arch;
import io.takari.jdkget.CachingOutput;
import io.takari.jdkget.ITransport;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.BinaryType;
import io.takari.jdkget.model.JdkRelease;
import io.takari.jdkget.model.JdkReleases;
import io.takari.jdkget.model.JdkVersion;

public class LoadAllIT {

  @Test
  @Ignore
  public void testDownloadUnpack() throws Exception {
    String ver = "9+181";

    JdkReleases releases = JdkReleases.readFromClasspath();
    ITransport transport = releases.createTransportFactory().createTransport();

    JdkRelease r = releases.select(JdkVersion.parse(ver));
    boolean failed = downloadUnpack(releases, r, Arch.WIN_64, true, transport);

    assertFalse(failed);
  }

  @Test
  public void testDownloadAndUnpackAll() throws IOException {
    String startWith = System.getProperty("io.takari.jdkget.startWith");
    JdkReleases releases = JdkReleases.readFromClasspath();
    ITransport transport = releases.createTransportFactory().createTransport();

    JdkVersion start = null;
    if (StringUtils.isNotBlank(startWith)) {
      start = releases.select(JdkVersion.parse(startWith)).getVersion();
    }

    boolean failed = false;

    for (JdkRelease r : releases.getReleases()) {
      if (start != null && r.getVersion().compareTo(start) > 0) {
        continue;
      }
      failed |= downloadUnpack(releases, r, null, true, transport);
    }

    assertFalse(failed);
  }

  private boolean downloadUnpack(JdkReleases releases, JdkRelease rel, Arch selArch, boolean unrestrictJce,
      ITransport transport) {
    JdkVersion ver = rel.getVersion();
    System.out.println(ver.longBuild() + " / " + ver.shortBuild() + (rel.isPsu() ? " PSU" : ""));
    Stream<Arch> a = selArch != null ? Stream.of(selArch) : rel.getArchs(BinaryType.JDK).parallelStream();

    boolean[] failed = new boolean[] {false};
    a.forEach(arch -> {
      for (BinaryType bt : BinaryType.values()) {
        CachingOutput o = new CachingOutput();
        try {

          JCE jce = null;
          if (unrestrictJce) {
            jce = releases.getJCE(rel.getVersion());
          }

          File jdktmp = new File("target/tmp/" + bt + "_" + arch);

          if (jdktmp.exists()) {
            FileUtils.forceDelete(jdktmp);
          }
          FileUtils.forceMkdir(jdktmp);

          if (rel.getUnpackableBinary(bt, arch) == null) {
            continue;
          }

          JdkGetter jdkGet = new JdkGetter(transport, o);
          jdkGet.setRemoveDownloads(false);
          jdkGet.get(rel, jce, arch, bt, jdktmp);

          System.out.println("  " + bt + " / " + arch + " >> OK");
        } catch (Throwable e) {
          failed[0] = true;
          System.err.println("  " + bt + " / " + arch + " >> FAIL");
          e.printStackTrace();
          o.error("", e);
          o.output(System.err);
          try (PrintStream out =
              new PrintStream(new File("target/tmp/" + rel.getVersion().toString() + "_" + bt + "_" + arch + "_error.log"))) {
            o.output(out);
          } catch (IOException ee) {
            ee.printStackTrace();
          }
        }
      }
    });

    Runtime rt = Runtime.getRuntime();
    long total = rt.totalMemory();
    long free = rt.freeMemory();
    long occBefore = total - free;

    System.gc();
    total = rt.totalMemory();
    free = rt.freeMemory();
    long occ = total - free;

    System.out.println("    MEM: " + (occ / 1024L / 1024L) + " (" + (occBefore / 1024L / 1024L) + ")");

    return failed[0];
  }
}
