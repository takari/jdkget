package io.takari.jdkget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;

public class JCEExtractor {

  public void extractJCE(File jceImage, File outputDirectory, File inProcessDirectory, IOutput output) throws IOException, InterruptedException {
    File secDir = new File(outputDirectory, "jre/lib/security");
    if (!secDir.exists()) {
      // osx
      secDir = new File(outputDirectory, "Contents/Home/jre/lib/security");
    }

    if (!secDir.exists()) {
      throw new IllegalStateException("Cannot find JCE target dir");
    }

    output.info("Installing unrestricted JCE policy files");
    unzip(jceImage, secDir, output);
  }

  private void unzip(File file, File dir, IOutput output) throws IOException, InterruptedException {
    ZipFile zip = new ZipFile(file);
    Enumeration<ZipArchiveEntry> entries = zip.getEntries();

    try {
      while (entries.hasMoreElements()) {

        ZipArchiveEntry e = entries.nextElement();

        String entryName = translateEntry(e.getName());
        if (!entryName.endsWith(".jar")) {
          continue;
        }

        File f = new File(dir, entryName);
        if (f.exists()) {
          FileUtils.forceDelete(f);
        }
        output.info("  Replacing " + entryName);
        try (InputStream zin = zip.getInputStream(e); OutputStream out = new FileOutputStream(f)) {
          Util.copyInterruptibly(zin, out);
        }
        f.setLastModified(e.getTime());
      }
    } finally {
      zip.close();
    }
  }

  private String translateEntry(String entryName) {
    entryName = entryName.replace('\\', '/');
    int pos = entryName.lastIndexOf('/');
    if (pos >= 0) {
      entryName = entryName.substring(pos + 1);
    }
    return entryName;
  }

}
