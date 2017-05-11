package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

public interface ITransport {

  void downloadJdk(JdkContext context, File jdkImage) throws IOException, InterruptedException;

  default void downloadJce(JdkContext context, File jceImage) throws IOException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  boolean validate(JdkContext context, File jdkImage) throws IOException, InterruptedException;

  File getImageFile(JdkContext context, File parent) throws IOException;

}
