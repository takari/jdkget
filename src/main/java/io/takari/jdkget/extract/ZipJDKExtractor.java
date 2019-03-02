package io.takari.jdkget.extract;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.model.JdkBinary;
import io.takari.jdkget.osx.PosixModes;

public class ZipJDKExtractor extends AbstractZipExtractor {

  @Override
  public boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir) throws IOException, InterruptedException {

    context.getLog().info("Extracting " + jdkImage.getName() + " into " + outputDir);

    String versionLine = bin.getRelease().getVersion().longVersion();
    outputDir.mkdir();

    try (InputStream in = new BufferedInputStream(new FileInputStream(jdkImage))) {
      ZipInputStream zip = new ZipInputStream(in);

      ZipEntry e;
      while ((e = zip.getNextEntry()) != null) {
        extractEntry(outputDir, versionLine, e, zip);
      }
    }

    // make sure bin files are executables
    if (File.pathSeparatorChar != ';') {
      updateExecutables(outputDir);
    }

    return true;
  }

  private void updateExecutables(File outputDir) throws IOException {
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
}
