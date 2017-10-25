package io.takari.jdkget.it;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import io.takari.jdkget.Arch;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.ITransport;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.JdkGetter.Builder;
import io.takari.jdkget.JdkReleases;
import io.takari.jdkget.JdkReleases.JdkRelease;
import io.takari.jdkget.JdkVersion;
import io.takari.jdkget.OracleWebsiteTransport;

public class LoadAllIT {

  @Test
  @Ignore
  public void testDownloadUnpack() throws Exception {
    String ver = "9+181";

    String otnu = System.getProperty("io.takari.jdkget.otn.username");
    String otnp = System.getProperty("io.takari.jdkget.otn.password");
    ITransport transport = new OracleWebsiteTransport(OracleWebsiteTransport.ORACLE_WEBSITE, otnu, otnp);
    JdkReleases releases = JdkReleases.readFromClasspath();

    JdkRelease r = releases.select(JdkVersion.parse(ver));
    boolean failed = downloadUnpack(releases, r, Arch.WIN_64, true, transport);

    assertFalse(failed);
  }

  @Test
  public void testDownloadAndUnpackAll() throws IOException {
    String startWith = System.getProperty("io.takari.jdkget.startWith");
    String otnu = System.getProperty("io.takari.jdkget.otn.username");
    String otnp = System.getProperty("io.takari.jdkget.otn.password");
    ITransport transport = new OracleWebsiteTransport(OracleWebsiteTransport.ORACLE_WEBSITE, otnu, otnp);
    JdkReleases releases = JdkReleases.readFromClasspath();

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

  private boolean downloadUnpack(JdkReleases releases, JdkRelease r, Arch selArch, boolean jce, ITransport transport) {
    JdkVersion v = r.getVersion();
    System.out.println(v.longBuild() + " / " + v.shortBuild() + (r.isPsu() ? " PSU" : ""));
    Stream<Arch> a = selArch != null ? Stream.of(selArch) : r.getArchs().parallelStream();

    boolean[] failed = new boolean[] {false};
    a.forEach(arch -> {
      File jdktmp = new File("target/tmp/" + arch);
      O o = new O();
      try {
        if (jdktmp.exists()) {
          FileUtils.forceDelete(jdktmp);
        }
        FileUtils.forceMkdir(jdktmp);

        Builder b = JdkGetter.builder() //
            .releases(releases) //
            .transport(transport) //
            .version(r.getVersion()) //
            .arch(arch) //
            .outputDirectory(jdktmp) //
            .output(o);
        if (jce) {
          b = b.unrestrictedJCE();
        }

        b.build().get();

        System.out.println("  " + arch + " >> OK");
        try (PrintStream out = new PrintStream(new File("target/tmp/" + v.toString() + "_" + arch + ".log"))) {
          o.output(out);
        }
      } catch (Throwable e) {
        failed[0] = true;
        System.err.println("  " + arch + " >> FAIL");
        o.output(System.err);
        e.printStackTrace();
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

  private static class O implements IOutput {

    private List<String> msgs = new ArrayList<>();

    @Override
    public void info(String message) {
      msgs.add("[INFO] " + message);
    }

    @Override
    public void error(String message) {
      msgs.add("[ERROR] " + message);
    }

    @Override
    public void error(String message, Throwable t) {
      error(message + ": " + t);
    }

    public void output(PrintStream out) {
      for (String msg : msgs) {
        out.println(msg);
      }
    }

  }
}
