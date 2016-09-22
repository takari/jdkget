package io.takari.jdkget.it;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import io.takari.jdkget.Arch;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.JdkReleases;
import io.takari.jdkget.JdkGetter.JdkVersion;
import io.takari.jdkget.JdkReleases.JdkRelease;

public class LoadAllIT {
  
  @Test
  public void testDownloadAndUnpackAll() throws IOException {
    
    for (JdkRelease r : JdkReleases.get().getReleases()) {

      JdkVersion v = r.getVersion();
      System.out.println(v.longBuild() + " / " + v.shortBuild() + (r.isPsu() ? " PSU" : ""));
      Stream<Arch> a = r.getArchs().parallelStream();

      a.forEach(arch -> {
        File jdktmp = new File("target/tmp/" + arch);
        O o = new O();
        try {
          if (jdktmp.exists()) {
            FileUtils.forceDelete(jdktmp);
          }
          FileUtils.forceMkdir(jdktmp);

          JdkGetter.builder()
            .version(r.getVersion())
            .arch(arch)
            .outputDirectory(jdktmp)
            .output(o)
            .build().get();

          System.out.println("  " + arch + " >> OK");
        } catch (Exception e) {
          System.err.println("  " + arch + " >> FAIL");
          o.output();
          e.printStackTrace();
        }
        try {
          FileUtils.forceDelete(jdktmp);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });

      System.gc();
      Runtime rt = Runtime.getRuntime();
      long total = rt.totalMemory();
      long free = rt.freeMemory();
      long occ = total - free;
      System.out.println("    MEM: " + (occ / 1024L / 1024L));
    }

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

    public void output() {
      for (String msg : msgs) {
        System.err.println(msg);
      }
    }

  }
}
