/*-
 * Copyright (C) 2006-2008 Erik Larsson
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

import java.util.HashMap;

import io.takari.osxjdkget.util.Util;

/**
 * This class adds concurrency safety to a random access stream. It includes a
 * seek+read atomic operation. All operations on this object is synchronized on
 * its own monitor.
 */
public class SynchronizedReadableRandomAccessStream
  extends BasicSynchronizedReadableRandomAccessStream
  implements SynchronizedReadableRandomAccess {

  private static final boolean DEBUG =
    Util.booleanEnabledByProperties(false,
      "org.catacombae.debug",
      "org.catacombae.io.debug",
      "org.catacombae.io." +
        SynchronizedReadableRandomAccessStream.class.getSimpleName() +
        ".debug");
  private static final boolean REFERENCES_DEBUG =
    Util.booleanEnabledByProperties(DEBUG,
      "org.catacombae.io." +
        SynchronizedReadableRandomAccessStream.class.getSimpleName() +
        ".references_debug");

  /** The underlying stream. */
  private ReadableRandomAccessStream ras;
  private long refCount;
  private boolean closed = false;
  private HashMap<Object, Reference> references =
    REFERENCES_DEBUG ? new HashMap<Object, Reference>() : null;

  public SynchronizedReadableRandomAccessStream(
    ReadableRandomAccessStream sourceStream) {
    this.ras = sourceStream;
    this.refCount = 1;

    if (REFERENCES_DEBUG) {
      references.put(this, new Reference(this,
        new Exception().getStackTrace()));
    }
  }

  /**
   * Returns the backing stream for this
   * SynchronizedReadableRandomAccessStream.
   * 
   * @return the backing stream for this
   * SynchronizedReadableRandomAccessStream.
   */
  public ReadableRandomAccessStream getSourceStream() {
    return ras;
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized int readFrom(final long pos, byte[] b, int off, int len)
    throws RuntimeIOException {
    if (DEBUG) {
      System.err.println(
        "SynchronizedReadableRandomAccessStream.readFrom(" + pos +
          ", byte[" + b.length + "], " + off + ", " + len + ");");
    }

    final long oldFP = getFilePointer();

    if (DEBUG) {
      System.err.println("  oldFP=" + oldFP);
    }

    if (oldFP != pos) {
      if (DEBUG) {
        System.err.println("  seeking to " + pos + "...");
      }

      seek(pos);
    }

    int res;

    try {
      if (DEBUG) {
        System.err.println("  Reading " + len + " bytes...");
      }

      res = read(b, off, len);

      if (DEBUG) {
        System.err.println("    read " + res + " bytes.");
      }
    } finally {
      if (DEBUG) {
        System.err.println("  seeking to " + oldFP + "...");
      }

      seek(oldFP); // Reset file pointer to previous position

    }

    if (DEBUG) {
      System.err.println("  returning " + res + ".");
    }

    return res;
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized long skipFrom(final long pos, final long length)
    throws RuntimeIOException {
    final long streamLength = length();
    final long newPos = pos + length;

    final long res;
    if (newPos > streamLength) {
      //seek(streamLength);
      res = streamLength - pos;
    } else {
      //seek(newPos);
      res = length;
    }

    return res;
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized long remainingLength() throws RuntimeIOException {
    return length() - getFilePointer();
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized void close() throws RuntimeIOException {
    if (DEBUG) {
      System.err.println(
        SynchronizedReadableRandomAccessStream.class.getName() +
          "@" + Util.toHexStringBE(hashCode()) + ".close(): Called " +
          "from " + new Exception().getStackTrace()[1] + ".");
    }

    if (closed) {
      throw new RuntimeException("Already closed.");
    }

    if (REFERENCES_DEBUG) {
      if (references.remove(this) == null) {
        throw new RuntimeException("Own reference not found!");
      }
    }

    --refCount;
    tryCloseSource();
    closed = true;
  }

  private void tryCloseSource() {
    if (refCount == 0) {
      ras.close();
    }
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized long getFilePointer() throws RuntimeIOException {
    return ras.getFilePointer();
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized long length() throws RuntimeIOException {
    return ras.length();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized int read() throws RuntimeIOException {
    return ras.read();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized int read(byte[] b) throws RuntimeIOException {
    return ras.read(b);
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized int read(byte[] b, int off, int len)
    throws RuntimeIOException {
    if (DEBUG) {
      System.err.println("SynchronizedReadableRandomAccessStream.read(" +
        "byte[" + b.length + "], " + off + ", " + len + ");");
      System.err.println("  ras=" + ras);
    }

    return ras.read(b, off, len);
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized void seek(long pos) throws RuntimeIOException {
    ras.seek(pos);
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized void addReference(Object referrer) {
    if (DEBUG) {
      System.err.println(this + ": Reference added (" + refCount + " " +
        "-> " + (refCount + 1) + ") by " + referrer + ".");
    }

    if (!closed) {
      if (REFERENCES_DEBUG) {
        if (references.get(referrer) != null) {
          throw new RuntimeException("Only one reference per " +
            "referrer is allowed.");
        }

        references.put(referrer, new Reference(referrer,
          new Exception().getStackTrace()));
      }

      ++refCount;
    } else
      throw new RuntimeIOException("Stream is closed!");
  }

  /** {@inheritDoc} */
  //@Override
  @Override
  public synchronized void removeReference(Object referrer) {
    if ((closed && refCount == 0) || (!closed && refCount == 1)) {
      throw new RuntimeException("No references!");
    }

    if (DEBUG) {
      System.err.println(this + ": Reference removed (" + refCount + " " +
        "-> " + (refCount - 1) + ") by " + referrer + ".");
    }

    if (REFERENCES_DEBUG) {
      if (references.remove(referrer) == null) {
        throw new RuntimeException("Reference not found!");
      }
    }

    --refCount;

    tryCloseSource();
  }

  @Override
  public synchronized void finalize() throws Throwable {
    try {
      if (refCount != 0) {
        System.err.println("[WARNING] " + this + " is garbage " +
          "collected with " + refCount + " remaining references" +
          (REFERENCES_DEBUG ? ":" : "."));
        if (REFERENCES_DEBUG) {
          for (Reference r : references.values()) {
            System.err.println(r.referrer);
            for (StackTraceElement ste : r.stackTrace) {
              System.err.println("\t" + ste);
            }
          }
        }
      }
    } finally {
      super.finalize();
    }
  }

  private class Reference {
    final Object referrer;
    final StackTraceElement[] stackTrace;

    public Reference(Object referrer, StackTraceElement[] stackTrace) {
      this.referrer = referrer;
      this.stackTrace = stackTrace;
    }
  }
}
