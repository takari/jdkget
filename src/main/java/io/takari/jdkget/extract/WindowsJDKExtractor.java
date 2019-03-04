package io.takari.jdkget.extract;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JdkBinary;
import io.takari.jdkget.win.CabEntry;
import io.takari.jdkget.win.CabInput;
import io.takari.jdkget.win.Cabinet;

public class WindowsJDKExtractor extends AbstractZipExtractor {

  @Override
  public boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir) throws IOException, InterruptedException {

    // <= 1.7: PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)
    // > 1.7: PE EXE <- PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)

    try (CabInput in = CabInput.fromFile(jdkImage)) {
      List<Cabinet> cabs = Cabinet.cabd_search(in);
      for (Cabinet cab : cabs) {
        for (CabEntry e : cab.entries()) {
          Util.checkInterrupt();
          if (e.getName().equals("tools.zip")) {

            // extract it
            outputDir.mkdirs();
            try (ZipInputStream zin = new ZipInputStream(e.getInputStream())) {
              ZipEntry ze = zin.getNextEntry();
              while (ze != null) {
                Util.checkInterrupt();
                extractEntry(outputDir, null, ze, zin);
                ze = zin.getNextEntry();
              }
            }

            return true;
          }
        }
      }
    }
    return false;
  }


}
