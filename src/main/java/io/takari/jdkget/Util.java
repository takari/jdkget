package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import io.takari.jdkget.model.JdkVersion;

public class Util {

  public static String cleanEntryName(String entryName, JdkVersion jdkVersion) {
    String release = jdkVersion.release();

    if (entryName.startsWith("./")) {
      entryName = entryName.substring(2);
    }
    if (entryName.isEmpty()) {
      return null;
    }

    int sl = entryName.indexOf('/');
    if (sl != -1) {
      String root = entryName.substring(0, sl);
      if (root.contains(release)) {
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
