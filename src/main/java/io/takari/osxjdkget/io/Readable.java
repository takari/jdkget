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
 * Defines the methods that must exist for a stream to be readable.
 * 
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public interface Readable {
  /**
   * Reads a single byte from the stream and returns it as an unsigned value (range 0-255). If the
   * end of stream has been reached, this method returns -1. This method should never return a
   * value outside the range <code>-1 &lt;= value &lt;=255</code>.
   * 
   * @return the next byte in the stream, or -1 if there are no more bytes to read.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if an I/O error occurred.
   */
  public int read() throws RuntimeIOException;

  /**
   * Reads as much data as possible from the stream into the array <code>data</code>, until the
   * end of the array has been reached. Returns the number of bytes that were read into <code>
   * data</code>. If no bytes could be read due to end of stream, -1 is returned.
   * 
   * @param data the array where the output data should be stored.
   * @return the number of bytes that were read, or -1 if no bytes could be read due to end of
   * stream.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if an I/O error occurred.
   */
  public int read(byte[] data) throws RuntimeIOException;

  /**
   * Reads as much data as possible from the stream into the array <code>data</code> at position
   * <code>pos</code>, until <code>len</code> bytes have been read. Returns the number of bytes
   * that were read into <code>data</code>. If no bytes could be read due to end of stream,
   * -1 is returned.
   * 
   * @param data the array where the output data should be stored.
   * @param pos the start position in the array where data should be stored.
   * @param len the amount of data to write into the array.
   * @return the number of bytes that were read, or -1 if no bytes could be read due to end of
   * stream.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if an I/O error occurred.
   */
  public int read(byte[] data, int pos, int len) throws RuntimeIOException;

  /**
   * Reads one byte from the stream and return it. If this is not possible due
   * to end of stream, a RuntimeIOException is thrown.
   *
   * @return the single byte which was read from the stream.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if the stream doesn't
   *     contain one more byte, or if an I/O error occurred.
   */
  public byte readFully() throws RuntimeIOException;

  /**
   * Reads into <code>data</code> until the end of the array has been reached. If this is not
   * possible due to end of stream, a RuntimeIOException is thrown.
   * 
   * @param data the array where the output data should be stored.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if the stream doesn't contain enough data to
   * fill the output array, or if an I/O error occurred.
   */
  public void readFully(byte[] data) throws RuntimeIOException;

  /**
   * Reads into <code>data</code> at position <code>pos</code> until <code>len</code> bytes have
   * been read. If this is not possible due to end of stream, a RuntimeIOException is thrown.
   * 
   * @param data the array where the output data should be stored.
   * @param offset the offset in the output array where we should start writing data.
   * @param length the number of bytes to write to the output array.
   * @throws io.takari.osxjdkget.io.RuntimeIOException if the stream doesn't contain enough data to
   * fill <code>len</code> bytes, or if an I/O error occurred.
   */
  public void readFully(byte[] data, int offset, int length) throws RuntimeIOException;
}
