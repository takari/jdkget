package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

public class SleepingTransport implements ITransport {

  @Override
  public void downloadJdk(JdkContext context, File jdkImage) throws IOException, InterruptedException {
    synchronized (this) {
      this.wait();
    }
  }

  @Override
  public boolean validate(JdkContext context, File jdkImage) throws IOException, InterruptedException {
    return true;
  }

  @Override
  public File getImageFile(JdkContext context, File parent) throws IOException {
    return new File(parent, "image");
  }

}
