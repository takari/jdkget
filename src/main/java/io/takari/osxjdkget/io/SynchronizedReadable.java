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
 * Interface that defines methods to access a ReadableRandomAccessStream in a thread-safe way.
 * 
 * @author Erik Larsson
 */
public interface SynchronizedReadable {
  /** Atomic seek+read. Does <b>not</b> change the file pointer of the stream permanently! */
  public int readFrom(final long pos) throws RuntimeIOException;

  /** Atomic seek+read. Does <b>not</b> change the file pointer of the stream permanently! */
  public int readFrom(final long pos, byte[] b) throws RuntimeIOException;

  /** Atomic seek+read. Does <b>not</b> change the file pointer of the stream permanently! */
  public int readFrom(final long pos, byte[] b, int off, int len) throws RuntimeIOException;

  /** Atomic seek+read. Does <b>not</b> change the file pointer of the stream permanently! */
  public void readFullyFrom(final long pos, byte[] data) throws RuntimeIOException;

  /** Atomic seek+read. Does <b>not</b> change the file pointer of the stream permanently! */
  public void readFullyFrom(final long pos, byte[] data, int offset, int length) throws RuntimeIOException;

  /** Atomic seek+skip. Does <b>not</b> change the file pointer of the stream permanently! */
  public long skipFrom(final long pos, final long length) throws RuntimeIOException;

  /** Atomic length() - getFilePointer(). */
  public long remainingLength() throws RuntimeIOException;
}
