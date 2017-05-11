package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

public class DumbTransport implements ITransport {
  private boolean valid;
  int tries;

  public DumbTransport(boolean valid) {
    this.valid = valid;
    tries = 0;
  }

  @Override
  public void downloadJdk(JdkContext context, File jdkImage) throws IOException, InterruptedException {
    jdkImage.createNewFile();
    tries++;
  }

  @Override
  public boolean validate(JdkContext context, File jdkImage) throws IOException, InterruptedException {
    return valid;
  }

  @Override
  public File getImageFile(JdkContext context, File parent) throws IOException {
    return new File("image");
  }
}