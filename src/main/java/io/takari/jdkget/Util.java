package io.takari.jdkget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import io.takari.jdkget.osx.PosixModes;

public class Util {

  public static void untar(InputStream in, String stripPrefix, File outputDir, IOutput log)
      throws IOException, InterruptedException, FileNotFoundException {
    TarArchiveInputStream t = new TarArchiveInputStream(in);
    TarArchiveEntry te;
    while ((te = t.getNextTarEntry()) != null) {

      checkInterrupt();

      String entryName = cleanEntryName(te.getName(), stripPrefix);
      if (entryName == null) {
        continue;
      }

      File f = new File(outputDir, entryName);
      if (te.isDirectory()) {
        f.mkdirs();
      } else {
        File parent = f.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }

        if (te.isSymbolicLink()) {
          if (File.pathSeparatorChar == ';') {
            log.info("Not creating symbolic link " + entryName + " -> " + te.getLinkName());
          } else {
            Path p = f.toPath();
            Files.createSymbolicLink(p, p.getParent().resolve(te.getLinkName()));
          }
        } else {
          try (OutputStream out = new FileOutputStream(f)) {
            copyInterruptibly(t, out);
          }
          if (File.pathSeparatorChar != ';') {
            int mode = (int) te.getMode() & 0000777;
            Files.setPosixFilePermissions(f.toPath(), PosixModes.intModeToPosix(mode));
          }
          f.setLastModified(te.getModTime().getTime());
        }
      }
    }
  }

  public static void unzip(InputStream in, String stripPrefix, File outputDir, IOutput log)
      throws InterruptedException, IOException {
    ZipInputStream zip = new ZipInputStream(in);

    ZipEntry e;
    while ((e = zip.getNextEntry()) != null) {
      checkInterrupt();
      extractZipEntry(outputDir, stripPrefix, e, zip);
    }
  }

  private static void extractZipEntry(File outputDir, String stripPrefix, ZipEntry e, InputStream zip)
      throws IOException, InterruptedException {

    boolean unpack200 = false;
    String name = e.getName();

    if (stripPrefix != null) {
      name = Util.cleanEntryName(name, stripPrefix);
      if (name == null) {
        return;
      }
    }

    if (name.endsWith(".pack")) {
      name = name.substring(0, name.length() - 5) + ".jar";
      unpack200 = true;
    }

    File f = new File(outputDir, name);
    if (e.isDirectory()) {

      f.mkdirs();

    } else {

      f.createNewFile();

      if (unpack200) {
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(f))) {
          // prevent unpacker from closing the stream
          InputStream zin = new FilterInputStream(zip) {
            @Override
            public void close() throws IOException {}
          };
          Pack200.newUnpacker().unpack(zin, out);
        }
      } else {
        try (OutputStream out = new FileOutputStream(f)) {
          Util.copyInterruptibly(zip, out);
        }
      }

    }
  }
  
  public static void updateExecutables(File outputDir) throws IOException {
    if (File.pathSeparatorChar == ';') {
      return; // skip windows
    }

    File bin = new File(outputDir, "bin");
    File[] binFiles = bin.listFiles();
    if (binFiles != null) {
      for (File ex : binFiles) {
        Path p = ex.toPath();
        int mode = PosixModes.posixToIntMode(Files.getPosixFilePermissions(p));
        Files.setPosixFilePermissions(p, PosixModes.intModeToPosix(mode | 0111)); // add +x
      }
    }
  }

  public static String cleanEntryName(String entryName, String stripPrefix) {
    if (entryName.startsWith("./")) {
      entryName = entryName.substring(2);
    }
    if (entryName.isEmpty()) {
      return null;
    }

    int sl = entryName.indexOf('/');
    if (sl != -1) {
      String root = entryName.substring(0, sl);
      if (root.contains(stripPrefix)) {
        entryName = entryName.substring(sl + 1);
      }
    }
    return entryName;
  }

  public static void copyInterruptibly(InputStream in, OutputStream out) throws IOException, InterruptedException {
    byte[] buf = new byte[4096];
    int l;
    while ((l = in.read(buf)) != -1) {
      checkInterrupt();
      out.write(buf, 0, l);
    }
  }

  public static void checkInterrupt() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }

  private static final long PROGRESS_FREQ = 3000L; // every 3 seconds

  public static void copyWithProgress(InputStream in, OutputStream out, long totalHint, IOutput output)
      throws IOException, InterruptedException {

    long start = System.currentTimeMillis();
    int progressChunk = 0;

    long totalBytes = totalHint;
    if (totalBytes == -1) {
      totalBytes = in.available();
    }
    long copiedBytes = 0;

    byte[] buf = new byte[4096];
    int l;
    while ((l = in.read(buf)) != -1) {
      checkInterrupt();
      out.write(buf, 0, l);
      copiedBytes += l;

      long time = System.currentTimeMillis() - start;
      int chunk = (int) (time / PROGRESS_FREQ);
      if (chunk > progressChunk) {
        progressChunk = chunk;
        output.printProgress(time, copiedBytes, totalBytes);
      }
    }
  }

  public static String timeToStr(long t) {
    StringBuilder sb = new StringBuilder();

    long h1 = TimeUnit.HOURS.toMillis(1);
    long m1 = TimeUnit.MINUTES.toMillis(1);
    if (t > h1) {
      long h = t / h1;
      t -= TimeUnit.HOURS.toMillis(h);
      sb.append(h).append("h");
    }

    if (t > m1 || sb.length() > 0) {
      long m = t / m1;
      t -= TimeUnit.MINUTES.toMillis(m);
      sb.append(m).append("m");
    }
    sb.append(t / 1000L).append("s");

    return sb.toString();
  }

}
