package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.JdkGetter.JdkVersion;

public interface ITransport {

  void downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException, InterruptedException;

  boolean validate(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException, InterruptedException;

  File getImageFile(File parent, Arch arch, JdkVersion version, IOutput output) throws IOException;
  
}
