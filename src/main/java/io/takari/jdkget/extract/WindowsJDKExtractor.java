package io.takari.jdkget.extract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dorkbox.cabParser.CabException;
import dorkbox.cabParser.CabParser;
import dorkbox.cabParser.CabStreamSaver;
import dorkbox.cabParser.structure.CabFileEntry;
import dorkbox.peParser.PE;
import dorkbox.peParser.headers.resources.ResourceDataEntry;
import dorkbox.peParser.headers.resources.ResourceDirectoryEntry;
import dorkbox.peParser.headers.resources.ResourceDirectoryHeader;
import dorkbox.peParser.misc.DirEntry;
import dorkbox.peParser.types.ImageDataDir;
import io.takari.jdkget.JdkContext;
import io.takari.jdkget.Util;

public class WindowsJDKExtractor extends AbstractZipExtractor {

  private static final Set<String> SKIP_DIRS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
    "Icon", "Bitmap")));

  @Override
  public boolean extractJdk(JdkContext context, File jdkImage, File outputDir, File workDir) throws IOException, InterruptedException {

    // <= 1.7: PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)
    // > 1.7:  PE EXE <- PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)

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

  private boolean scanPE(File f, File outputDir, File workDir) throws IOException, InterruptedException {
    PE pe;
    try {
      pe = new PE(f.getCanonicalPath());
    } catch (Exception e) {
      return false;
    }

    if (pe.isPE()) {
      for (ImageDataDir entry : pe.optionalHeader.tables) {
        Util.checkInterrupt();
        if (entry.getType() == DirEntry.RESOURCE) {

          ResourceDirectoryHeader root = (ResourceDirectoryHeader) entry.data;
          if (scanPEDir(pe, root, outputDir, workDir)) {
            return true;
          }

        }
      }

    }
    return false;
  }

  private boolean scanPEDir(PE pe, ResourceDirectoryHeader dir, File outputDir, File workDir) throws IOException, InterruptedException {
    for (ResourceDirectoryEntry entry : dir.entries) {
      Util.checkInterrupt();
      if (entry.isDirectory) {
        if (SKIP_DIRS.contains(entry.NAME.get())) {
          continue;
        }
        if (scanPEDir(pe, entry.directory, outputDir, workDir)) {
          return true;
        }
      } else {
        if (scanPEEntry(pe, entry.resourceDataEntry, outputDir, workDir)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean scanPEEntry(PE pe, ResourceDataEntry resourceDataEntry, File outputDir, File workDir) throws IOException, InterruptedException {

    byte[] data = resourceDataEntry.getData(pe.fileBytes);
    if (scanCab(outputDir, data)) {
      return true;
    }

    // try pe
    File f = new File(workDir, "cabextract-" + System.currentTimeMillis() + ".tmp");
    f.createNewFile();
    try {
      try (OutputStream out = new FileOutputStream(f)) {
        out.write(data);
      }
      if (scanPE(f, outputDir, workDir)) {
        return true;
      }
    } finally {
      if (!f.delete()) {
        f.deleteOnExit();
      }
    }

    return false;
  }

  private boolean scanCab(final File output, byte[] data) throws IOException {
    final boolean[] res = new boolean[] {false};
    InputStream in = new ByteArrayInputStream(data);
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
              output.createNewFile();
              return new FileOutputStream(output);
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
      // not a cab file, probably
    }
    return res[0];
  }
}
