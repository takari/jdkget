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

package io.takari.jdkget.osx.io;

/**
 * Defines the methods that must exist for a stream to be seekable in a random access way.
 * 
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public interface RandomAccess {
  /**
   * Repositions the stream to a specific byte position, counted from the start of the stream. The
   * next read from the stream will start at byte position <code>pos</code>.
   * 
   * @param pos the new stream position.
   * @throws io.takari.jdkget.osx.io.RuntimeIOException if an I/O error occurred.
   */
  public void seek(long pos) throws RuntimeIOException;

  /**
   * Returns the length of the stream in bytes, counted from the start. Maximum seekable position
   * will be this value.
   * 
   * @return the length of the stream in bytes.
   * @throws io.takari.jdkget.osx.io.RuntimeIOException if an I/O error occurred.
   */
  public long length() throws RuntimeIOException;

  /**
   * Returns the current byte position in the stream. This method is named getFilePointer()
   * despite that the stream may not be backed by a file in order to maintain interchangeability
   * with java.io.RandomAccessFile .
   * 
   * @return the current byte position in the stream.
   * @throws io.takari.jdkget.osx.io.RuntimeIOException if an I/O error occurred.
   */
  public long getFilePointer() throws RuntimeIOException;
}
