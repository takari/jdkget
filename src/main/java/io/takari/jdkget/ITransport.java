package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.JdkBinary;

public interface ITransport {

  void downloadJdk(JdkGetter context, JdkBinary binary, File jdkImage) throws IOException, InterruptedException;

  default void downloadJce(JdkGetter context, JCE jce, File jceImage) throws IOException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  boolean validate(JdkGetter context, JdkBinary binary, File jdkImage) throws IOException, InterruptedException;

}
