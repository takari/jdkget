package io.takari.jdkget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;

public class JCEExtractor {

  public void extractJCE(JdkContext context, File jceImage, File outputDir, File workDir) throws IOException, InterruptedException {
    File secDir = new File(outputDir, "jre/lib/security");
    if (!secDir.exists()) {
      throw new IllegalStateException("Cannot find JCE target dir");
    }

    context.getOutput().info("Installing unrestricted JCE policy files");
    unzip(jceImage, secDir, context.getOutput());
  }

  public void fixJce(JdkContext context, File outputDir) throws IOException {
    File secDir = new File(outputDir, "jre/lib/security");
    if (!secDir.exists()) {
      throw new IllegalStateException("Cannot find JCE target dir");
    }

    context.getOutput().info("Unrestricting JCE policy");
    File security = new File(secDir, "java.security");
    if (!security.isFile()) {
      throw new IOException("Cannot unrestrict JCE policy, no java.security file found");
    }

    boolean found = false;
    List<String> lines = FileUtils.readLines(security);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.contains("crypto.policy=unlimited") && line.startsWith("#")) {
        lines.set(i, line.substring(1));
        found = true;
        break;
      }
    }
    if (!found) {
      lines.add("crypto.policy=unlimited");
    }
    FileUtils.writeLines(security, lines);
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
