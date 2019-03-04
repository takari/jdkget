package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.model.JdkBinary;

public class DumbTransport implements ITransport {
  private boolean valid;
  int tries;

  public DumbTransport(boolean valid) {
    this.valid = valid;
    tries = 0;
  }

  @Override
  public void downloadJdk(JdkGetter context, JdkBinary binary, File jdkImage)
      throws IOException, InterruptedException {
    jdkImage.createNewFile();
    tries++;
  }

  @Override
  public boolean validate(JdkGetter context, JdkBinary binary, File jdkImage)
      throws IOException, InterruptedException {
    return valid;
  }

}
