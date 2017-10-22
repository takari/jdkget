package io.takari.jdkget.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.katjahahn.parser.Location;
import com.github.katjahahn.parser.PEData;
import com.github.katjahahn.parser.PELoader;
import com.github.katjahahn.parser.sections.SectionLoader;
import com.github.katjahahn.parser.sections.rsrc.Resource;
import com.github.katjahahn.parser.sections.rsrc.ResourceSection;

import dorkbox.cabParser.CabException;
import dorkbox.cabParser.CabParser;
import dorkbox.cabParser.CabStreamSaver;
import dorkbox.cabParser.structure.CabFileEntry;
import io.takari.jdkget.JdkContext;
import io.takari.jdkget.Util;

public class WindowsJDKExtractor extends AbstractZipExtractor {

  public static final int MSCF = 0x4D534346;

  @Override
  public boolean extractJdk(JdkContext context, File jdkImage, File outputDir, File workDir) throws IOException, InterruptedException {

    // <= 1.7: PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)
    // > 1.7: PE EXE <- PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)

    File tmptools = new File(workDir, "tools-" + System.currentTimeMillis() + ".zip");
    if (scanPE(jdkImage, tmptools, workDir)) {
      Util.checkInterrupt();
      try {
        context.getOutput().info("Extracting tools.zip from install executable into " + outputDir);
        extractTools(tmptools, outputDir);
        return true;
      } finally {
        if (!tmptools.delete()) {
          tmptools.deleteOnExit();
        }
      }
    } else {
      context.getOutput().error("This doesn't seem to be a PE executable");
    }
    return false;
  }

  private void extractTools(File toolZip, File output) throws IOException, InterruptedException {
    output.mkdirs();

    try (ZipFile zip = new ZipFile(toolZip)) {
      Enumeration<? extends ZipEntry> en = zip.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = en.nextElement();

        try (InputStream ein = zip.getInputStream(e)) {
          extractEntry(output, null, e, ein);
        }
      }
    }
  }

  private boolean scanPE(File f, File outputFile, File workDir) throws IOException, InterruptedException {
    PEData data = PELoader.loadPE(f);
    ResourceSection rsrc = new SectionLoader(data).loadResourceSection();
    List<Resource> resources = rsrc.getResources();

    for (Resource res : resources) {
      Location loc = res.rawBytesLocation();
      long offset = loc.from();
      // this example only works for small resources
      assert loc.size() == (int) loc.size();
      int size = (int) loc.size();
      try (RandomAccessFile raf = new RandomAccessFile(data.getFile(), "r")) {
        raf.seek(offset);
        int sig = raf.readInt();
        raf.seek(offset);

        if (sig == MSCF) {
          File cab = new File(workDir, "cab-" + System.currentTimeMillis() + ".tmp");
          extractFileFromExe(raf, size, cab);
          if (scanCab(outputFile, cab)) {
            return true;
          }
        }

        if ((sig & 0xffff0000) == ('M' << 24 | 'Z' << 16)) {
          // try pe
          File subf = new File(workDir, "exe-" + System.currentTimeMillis() + ".tmp");
          extractFileFromExe(raf, size, subf);
          try {
            if (scanPE(subf, outputFile, workDir)) {
              return true;
            }
          } finally {
            if (!subf.delete()) {
              subf.deleteOnExit();
            }
          }
        }

      }
    }

    return false;
  }

  private void extractFileFromExe(RandomAccessFile raf, int size, File output) throws IOException, FileNotFoundException {
    try (OutputStream out = new FileOutputStream(output)) {
      byte[] buf = new byte[32784];
      int total = size;
      int l;
      while (total > 0) {
        l = raf.read(buf, 0, Math.min(total, buf.length));
        out.write(buf, 0, l);
        total -= l;
      }
    }
  }

  private boolean scanCab(File outputFile, File cab) throws IOException {
    OutputStream[] out = new OutputStream[] {null};
    boolean[] res = new boolean[] {false};
    try (InputStream in = new FileInputStream(cab)) {
      try {
        new CabParser(in, new CabStreamSaver() {
          public boolean saveReservedAreaData(byte[] data, int dataLength) {
            return false;
          }

          public OutputStream openOutputStream(CabFileEntry entry) {
            if (res[0])
              return null;

            if (entry.getName().equals("tools.zip")) {
              res[0] = true;
              try {
                outputFile.createNewFile();
                out[0] = new FileOutputStream(outputFile);
                return out[0];
              } catch (IOException e) {
                return null;
              }
            }
            return null;
          }

          public void closeOutputStream(OutputStream os, CabFileEntry entry) {
            try {
              os.close();
            } catch (IOException e) {
            }
          }
        }).extractStream();
      } catch (CabException e) {
        throw new IOException(e);
      }
    } finally {
      if (out[0] != null) {
        out[0].close();
      }
    }
    return res[0];
  }

}
