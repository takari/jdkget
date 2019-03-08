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

import io.takari.jdkget.model.BinaryType;

public class JCEExtractor {

  private static final String CRYPTO_POLICY_UNLIMITED = "crypto.policy=unlimited";
  private static final String CRYPTO_POLICY_UNLIMITED_COMMENTED = '#' + CRYPTO_POLICY_UNLIMITED;

  public void extractJCE(JdkGetter context, BinaryType type, File jceImage, File outputDir)
      throws IOException, InterruptedException {
    File secDir = getSecDir(type, outputDir);

    context.getLog().info("Installing unrestricted JCE policy files");
    unzip(jceImage, secDir, context.getLog());
  }

  public void fixJce(JdkGetter context, BinaryType type, File outputDir) throws IOException {
    File secDir = getSecDir(type, outputDir);

    context.getLog().info("Unrestricting JCE policy");
    File security = new File(secDir, "java.security");
    if (!security.isFile()) {
      throw new IOException("Cannot unrestrict JCE policy, no java.security file found");
    }

    boolean found = false;
    List<String> lines = FileUtils.readLines(security);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.equals(CRYPTO_POLICY_UNLIMITED)) {
        found = true;
        break;
      }
      if (line.equals(CRYPTO_POLICY_UNLIMITED_COMMENTED)) {
        lines.set(i, line.substring(1));
        found = true;
        break;
      }
    }
    if (!found) {
      lines.add(CRYPTO_POLICY_UNLIMITED);
    }
    FileUtils.writeLines(security, lines);
  }

  private File getSecDir(BinaryType type, File outputDir) {
    String secPath = "lib/security";
    if (type == BinaryType.JDK || type == BinaryType.SERVERJRE) {
      secPath = "jre/lib/security";
    }

    File secDir = new File(outputDir, secPath);
    if (!secDir.exists()) {
      throw new IllegalStateException("Cannot find JCE target dir " + secPath);
    }
    return secDir;
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
