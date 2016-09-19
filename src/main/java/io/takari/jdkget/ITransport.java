package io.takari.jdkget;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.takari.jdkget.JdkGetter.JdkVersion;

public interface ITransport {

  boolean downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException;
  
  List<JdkVersion> listVersions() throws IOException;

}
