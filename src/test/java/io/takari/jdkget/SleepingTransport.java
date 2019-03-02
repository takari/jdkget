package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.model.JdkBinary;

public class SleepingTransport implements ITransport {

  @Override
  public void downloadJdk(JdkGetter context, JdkBinary binary, File jdkImage)
      throws IOException, InterruptedException {
    synchronized (this) {
      this.notify();
      this.wait();
    }
  }

  @Override
  public boolean validate(JdkGetter context, JdkBinary binary, File jdkImage)
      throws IOException, InterruptedException {
    return true;
  }

}
