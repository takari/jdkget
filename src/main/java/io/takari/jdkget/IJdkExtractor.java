package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.JdkGetter.JdkVersion;

public interface IJdkExtractor {
  
  boolean extractJdk(JdkVersion version, File jdkImage, File outputDir, File workDir, IOutput output) throws IOException;
  
}
