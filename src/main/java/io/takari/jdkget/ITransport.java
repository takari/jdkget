package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

import io.takari.jdkget.JdkGetter.JdkVersion;

public interface ITransport {

  void downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage) throws IOException;

}
