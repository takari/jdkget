package io.takari.jdkget.extract;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JdkBinary;

public class BinJDKExtractor implements IJdkExtractor {

  private static final int[] ZIP_PREFIX = new int[] {0x50, 0x4b, 0x03, 0x04};
  private static final int[] GZIP_PREFIX = new int[] {0x1f, 0x8b, 0x08};

  private static final int MAX_PREFIX_READ = 0x20000;

  @Override
  public boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir)
      throws IOException, InterruptedException {

    context.getLog().info("Extracting " + jdkImage.getName() + " into " + outputDir);
    outputDir.mkdir();

    try (InputStream in = new BufferedInputStream(new FileInputStream(jdkImage))) {

      // find start of zip
      if (findBinaryStream(in, ZIP_PREFIX)) {

        Util.unzip(in, bin.getRelease().getVersion().release(), outputDir, context.getLog());

      } else if (findBinaryStream(in, GZIP_PREFIX)) {

        Util.untar(new GZIPInputStream(in), bin.getRelease().getVersion().release(), outputDir, context.getLog());

      } else {
        throw new IllegalStateException("Cannot find start of archive stream");
      }

    }

    // make sure bin files are executables
    Util.updateExecutables(outputDir);

    return true;
  }

  private boolean findBinaryStream(InputStream in, int[] prefix) throws IOException {

    int total = 0;
    int idx = 0;
    while (true) {
      if (idx == 0) {
        in.mark(prefix.length);
      }

      int b = in.read();
      if (b == -1) {
        break;
      }
      total++;
      if (b == prefix[idx]) {
        idx++;
        if (idx >= prefix.length) {
          // found it!
          in.reset();
          return true;
        }
      } else {
        idx = 0;
      }

      if (total > MAX_PREFIX_READ) {
        break;
      }
    }
    return false;
  }

}
