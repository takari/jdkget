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
 * This interface defines the methods that must exist for a stream to be writable.
 * 
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public interface Writable {
  /**
   * Writes the complete contents of <code>data</code> to the stream, at its current position.
   * 
   * @param data array containing the data to write to the stream.
   */
  public void write(byte[] data) throws RuntimeIOException;

  /**
   * Writes a subset of <code>data</code> to the stream, starting at array offset <code>pos</code>
   * and writing <code>len</code> bytes in total.
   * 
   * @param data array containing the data to write to the stream.
   * @param off the offset in <code>data</code> to start reading.
   * @param len the number of bytes to write to the stream.
   */
  public void write(byte[] data, int off, int len) throws RuntimeIOException;

  /**
   * Writes a single byte to the stream. <code>data</code> will be unsigned first, so valid ranges
   * are <code>0 &lt;= data &lt;= 255</code> or <code>-128 &lt;= data &lt;= 127</code>.
   * 
   * @param data integer containing the single byte to write to the stream.
   */
  public void write(int data) throws RuntimeIOException;

}
