package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

public interface IJdkExtractor {
  
  boolean extractJdk(JdkContext context, File jdkImage, File outputDir, File workDir) throws IOException, InterruptedException;
  
}
