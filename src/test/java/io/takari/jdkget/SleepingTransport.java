package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.JdkGetter.JdkVersion;

public class SleepingTransport implements ITransport {

  @Override
  public void downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException, InterruptedException {
    synchronized (this) {
      this.wait();
    }
  }

  @Override
  public boolean validate(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException, InterruptedException {
    return true;
  }

  @Override
  public File getImageFile(File parent, Arch arch, JdkVersion version) throws IOException {
    return new File(parent, "image");
  }

}
