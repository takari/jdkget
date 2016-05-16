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

package io.takari.osxjdkget.io;

/**
 * A ReadableRandomAccessStream implementation backed by a byte array.
 * 
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public class ReadableByteArrayStream extends BasicReadableRandomAccessStream {
  private final byte[] backingArray;
  private final int startOffset;
  private final int length;
  private int filePointer;
  private boolean closed = false;

  public ReadableByteArrayStream(byte[] array) {
    this(array, 0, array.length);
  }

  public ReadableByteArrayStream(byte[] array, int off, int len) {
    if (off >= array.length || off < 0)
      throw new IllegalArgumentException("parameter off out of bounds (off=" + off + ")");
    if (off + len > array.length || len < 0)
      throw new IllegalArgumentException("parameter len out of bounds (len=" + len + ")");
    this.backingArray = array;
    this.startOffset = off;
    this.length = len;
    this.filePointer = 0;
  }

  @Override
  public void seek(long pos) {
    if (closed)
      throw new RuntimeException("File has been closed!");

    if (pos >= length || pos < 0)
      throw new IllegalArgumentException("parameter pos out of bounds");
    else
      filePointer = (int) pos;
  }

  @Override
  public int read(byte[] data, int pos, int len) {
    if (closed)
      throw new RuntimeException("File has been closed!");

    int remainingBytes = length - filePointer;
    if (remainingBytes == 0)
      return -1;

    int trueLen = Math.min(remainingBytes, len);
    System.arraycopy(backingArray, startOffset + filePointer, data, pos, trueLen);
    filePointer += trueLen;
    return trueLen;
  }

  @Override
  public long length() {
    if (closed)
      throw new RuntimeException("File has been closed!");

    return length;
  }

  @Override
  public long getFilePointer() {
    if (closed)
      throw new RuntimeException("File has been closed!");

    return filePointer;
  }

  @Override
  public void close() {
    if (closed)
      throw new RuntimeException("File has been closed!");

    closed = true;
  }

}
