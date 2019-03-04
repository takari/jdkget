package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.model.JdkBinary;

public interface IJdkExtractor {
  
  boolean extractJdk(JdkGetter context, JdkBinary bin, File jdkImage, File outputDir) throws IOException, InterruptedException;
  
}
