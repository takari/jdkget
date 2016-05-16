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

import java.io.IOException;
import java.io.InputStream;

/**
 * Transforms a java.io.InputStream into an org.catacombae.io.Readable.
 *
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public class InputStreamReadable extends BasicReadable implements Stream {

  private final InputStream is;

  public InputStreamReadable(InputStream is) {
    this.is = is;
  }

  /** {@inheritDoc} */
  @Override
  public int read(byte[] data, int pos, int len) throws RuntimeIOException {
    try {
      return is.read(data, pos, len);
    } catch (IOException ex) {
      throw new RuntimeIOException(ex);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws RuntimeIOException {
    try {
      is.close();
    } catch (IOException ex) {
      throw new RuntimeIOException(ex);
    }
  }
}
