package io.takari.jdkget.win;

import java.io.IOException;
import java.io.InputStream;

public interface ByteSource {

  /**
   * @see InputStream#read(byte[], int, int)
   */
  int read(byte[] buf, int offset, int len) throws IOException;

}
