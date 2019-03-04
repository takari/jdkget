package io.takari.jdkget.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JdkVersion;

public abstract class AbstractZipExtractor implements IJdkExtractor {

  protected void extractEntry(File outputDir, JdkVersion version, ZipEntry e, InputStream zip)
      throws IOException, InterruptedException {
    Util.checkInterrupt();

    boolean unpack200 = false;
    String name = e.getName();

    if (version != null) {
      name = Util.cleanEntryName(name, version);
      if (name == null) {
        return;
      }
    }

    if (name.endsWith(".pack")) {
      name = name.substring(0, name.length() - 5) + ".jar";
      unpack200 = true;
    }

    File f = new File(outputDir, name);
    if (e.isDirectory()) {

      f.mkdirs();

    } else {

      f.createNewFile();

      if (unpack200) {
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(f))) {
          // prevent unpacker from closing the stream
          InputStream zin = new FilterInputStream(zip) {
            @Override
            public void close() throws IOException {}
          };
          Pack200.newUnpacker().unpack(zin, out);
        }
      } else {
        try (OutputStream out = new FileOutputStream(f)) {
          Util.copyInterruptibly(zip, out);
        }
      }

    }
  }
}
