package io.takari.jdkget.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JdkBinary;

public abstract class AbstractTarJDKExtractor implements IJdkExtractor {

  protected abstract InputStream wrap(InputStream in) throws IOException;

  @Override
  public boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir)
      throws IOException, InterruptedException {

    context.getLog().info("Extracting " + jdkImage.getName() + " image into " + outputDir);

    try (InputStream in = new FileInputStream(jdkImage)) {
      Util.untar(wrap(in), bin.getRelease().getVersion().release(), outputDir, context.getLog());
    }

    return true;
  }

}
