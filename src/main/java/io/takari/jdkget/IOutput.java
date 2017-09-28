package io.takari.jdkget;

public interface IOutput {
  
  void info(String message);
  
  void error(String message);
  
  void error(String message, Throwable t);
  
  default void printProgress(long time, long copiedBytes, long totalBytes) {

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

      info(String.format("Downloading %,.2f %s of %,.2f %s (%.2f%%) @ %,.2f %s ETA: %s", copied, copiedUnit, total, totalUnit, pct, spd, spdUnit, Util.timeToStr(eta)));
    } else {
      info(String.format("Downloading %,.2f %s @ %,.2f %s", copied, copiedUnit, spd, spdUnit));
    }
  }

}
