package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.JdkGetter.JdkVersion;

public class DumbTransport implements ITransport {
  private boolean valid;
  int tries;

  public DumbTransport(boolean valid) {
    this.valid = valid;
    tries = 0;
  }

  @Override
  public void downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {
    jdkImage.createNewFile();
    tries++;
  }

  @Override
  public boolean validate(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {
    return valid;
  }

  @Override
  public File getImageFile(File parent, Arch arch, JdkVersion version) throws IOException {
    return new File("image");
  }
}