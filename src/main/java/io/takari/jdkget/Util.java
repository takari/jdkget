package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {

  public static void copyInterruptibly(InputStream in, OutputStream out) throws IOException, InterruptedException {
    byte[] buf = new byte[4096];
    int l;
    while ((l = in.read(buf)) != -1) {
      checkInterrupt();
      out.write(buf, 0, l);
    }
  }

  public static void checkInterrupt() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }

}
