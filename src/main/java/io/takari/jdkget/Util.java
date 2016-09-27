package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class Util {

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

  public static void copyWithProgress(InputStream in, OutputStream out, long totalHint, IOutput output) throws IOException, InterruptedException {

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
        printProgress(time, copiedBytes, totalBytes, output);
      }
    }
  }

  private static void printProgress(long time, long copiedBytes, long totalBytes, IOutput output) {

    // spd
    long sec = time / 1000L;
    double spd = (((double) copiedBytes) / sec) / 1024; // kb/s
    String spdUnit = "kB/s";
    if (spd > 1024) {
      spd /= 1024;
      spdUnit = "MB/s";
    }

    // copied
    double copied = ((double) copiedBytes) / 1024; // kb
    String copiedUnit = "kB";
    if (copied > 1024) {
      copied /= 1024;
      copiedUnit = "MB";
    }
    if (copied > 1024) {
      copied /= 1024;
      copiedUnit = "GB";
    }

    // total
    if (totalBytes > 0) {
      double total = ((double) totalBytes) / 1024; // kb
      String totalUnit = "kB";
      if (total > 1024) {
        total /= 1024;
        totalUnit = "MB";
      }
      if (total > 1024) {
        total /= 1024;
        totalUnit = "GB";
      }

      double pct = ((double) copiedBytes * 100) / totalBytes;
      long eta = ((time * (totalBytes - copiedBytes)) / copiedBytes);

      output.info(String.format("Downloading %,.2f %s of %,.2f %s (%.2f%%) @ %,.2f %s ETA: %s", copied, copiedUnit, total, totalUnit, pct, spd, spdUnit, timeToStr(eta)));
    } else {
      output.info(String.format("Downloading %,.2f %s @ %,.2f %s", copied, copiedUnit, spd, spdUnit));
    }
  }

  private static String timeToStr(long t) {
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
