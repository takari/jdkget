package io.takari.jdkget.extract;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JdkBinary;

public class ZipJDKExtractor implements IJdkExtractor {

  @Override
  public boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir)
      throws IOException, InterruptedException {

    context.getLog().info("Extracting " + jdkImage.getName() + " into " + outputDir);

    outputDir.mkdir();

    try (InputStream in = new BufferedInputStream(new FileInputStream(jdkImage))) {
      Util.unzip(in, bin.getRelease().getVersion().release(), outputDir, context.getLog());
    }

    // make sure bin files are executables
    Util.updateExecutables(outputDir);

    return true;
  }

}
