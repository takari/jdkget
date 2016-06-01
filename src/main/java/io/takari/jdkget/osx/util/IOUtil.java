/*-
 * Copyright (C) 2008 Erik Larsson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.takari.jdkget.osx.util;

import io.takari.jdkget.osx.io.ReadableRandomAccessStream;
import io.takari.jdkget.osx.io.RuntimeIOException;

/**
 * CatacombaeIO-specific utility class.
 * 
 * @author Erik Larsson
 */
public class IOUtil {
  /**
   * Reads the supplied ReadableRandomAccessStream from its current position
   * until the end of the stream.
   *
   * @param s
   * @return the contents of the remainder of the stream.
   * @throws io.takari.jdkget.osx.io.RuntimeIOException if an I/O error occurred
   * when reading the stream.
   */
  public static byte[] readFully(ReadableRandomAccessStream s)
    throws RuntimeIOException {
    if (s.length() < 0 || s.length() > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Length of s is out of range: " +
        s.length());
    }

    return readFully(s, null, (int) s.length());
  }

  public static byte[] readFully(ReadableRandomAccessStream s, long offset,
    int length) throws RuntimeIOException {
    return readFully(s, Long.valueOf(offset), length);
  }

  private static byte[] readFully(ReadableRandomAccessStream s, Long offset,
    int length) throws RuntimeIOException {
    long trueOffset;

    if (offset == null) {
      trueOffset = s.getFilePointer();
    } else {
      trueOffset = offset;
    }

    if (length > s.length()) {
      throw new IllegalArgumentException("'length' is unreasonably " +
        "large: " + length);
    } else if ((s.length() - length) < trueOffset) {
      throw new IllegalArgumentException("Offset out of range: " +
        trueOffset + "(length: " + s.length() + ")");
    }

    byte[] res = new byte[length];

    if (offset != null) {
      s.seek(trueOffset);
    }

    s.readFully(res);

    return res;
  }
}
