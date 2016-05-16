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
 * Basic implementation of convenience methods in a SynchronizedReadableRandomAccess.
 * 
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public abstract class BasicSynchronizedReadableRandomAccessStream extends BasicReadableRandomAccessStream implements SynchronizedReadableRandomAccess {
  /** {@inheritDoc} */
  //@Override
  @Override
  public int readFrom(long pos) throws RuntimeIOException {
    byte[] res = new byte[1];
    if (readFrom(pos, res, 0, 1) == 1)
      return res[0] & 0xFF;
    else
      return -1;
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public int readFrom(long pos, byte[] b) throws RuntimeIOException {
    return readFrom(pos, b, 0, b.length);
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public void readFullyFrom(long pos, byte[] data) throws RuntimeIOException {
    readFullyFrom(pos, data, 0, data.length);
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public void readFullyFrom(long pos, byte[] data, int offset, int length) throws RuntimeIOException {
    if (length < 0)
      throw new IllegalArgumentException("length is negative: " + length);
    int bytesRead = 0;
    while (bytesRead < length) {
      int curBytesRead = readFrom(pos + bytesRead, data, offset + bytesRead, length - bytesRead);
      if (curBytesRead > 0)
        bytesRead += curBytesRead;
      else
        throw new RuntimeIOException("Couldn't read the entire length.");
    }
  }
}
