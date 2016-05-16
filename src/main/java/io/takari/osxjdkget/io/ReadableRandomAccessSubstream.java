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

import io.takari.osxjdkget.util.Util;

/**
 * A substream class using a SynchronizedReadableRandomAccess as source for a
 * completely independent stream with its own file pointer and access to the
 * same data.
 * 
 * @author Erik Larsson
 */
public class ReadableRandomAccessSubstream extends BasicReadableRandomAccessStream {
  private static final boolean DEBUG =
    Util.booleanEnabledByProperties(false,
      "org.catacombae.debug",
      "org.catacombae.io.debug",
      "org.catacombae.io." +
        ReadableRandomAccessSubstream.class.getSimpleName() + ".debug");

  private SynchronizedReadableRandomAccess sourceStream;
  private long internalFP;
  private boolean closed = false;

  public ReadableRandomAccessSubstream(SynchronizedReadableRandomAccess iSourceStream) {
    this.sourceStream = iSourceStream;
    this.internalFP = 0;

    sourceStream.addReference(this);
  }

  @Override
  public synchronized void close() throws RuntimeIOException {
    if (closed) {
      throw new RuntimeException(this + " already closed!");
    }

    sourceStream.removeReference(this);
    closed = true;
  }

  @Override
  public void seek(long pos) throws RuntimeIOException {
    internalFP = pos;
  }

  @Override
  public long length() throws RuntimeIOException {
    return sourceStream.length();
  }

  @Override
  public long getFilePointer() throws RuntimeIOException {
    return internalFP;
  }

  @Override
  public int read(byte[] b, int pos, int len) throws RuntimeIOException {
    if (DEBUG) {
      System.err.println("ReadableRandomAccessSubstream.read(byte[" +
        b.length + "], " + pos + ", " + len + ");");
      System.err.println("  readFrom: " + internalFP);
    }

    int bytesRead = sourceStream.readFrom(internalFP, b, pos, len);
    if (bytesRead > 0) {
      internalFP += bytesRead;

      if (DEBUG) {
        System.err.println("  returning: " + bytesRead);
      }

      return bytesRead;
    } else {
      if (DEBUG) {
        System.err.println("  returning: -1");
      }

      return -1;
    }
  }

}
