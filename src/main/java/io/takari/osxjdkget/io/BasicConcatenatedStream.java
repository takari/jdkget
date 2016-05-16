/*-
 * Copyright (C) 2007-2008 Erik Larsson
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import io.takari.osxjdkget.util.Util;

/**
 * Common superclass of ReadableConcatenatedStream and ConcatenatedStream.
 *
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public abstract class BasicConcatenatedStream<A extends ReadableRandomAccessStream>
  extends BasicReadableRandomAccessStream {

  private static final IOLog log = IOLog.getInstance();

  static {
    log.debug = Util.booleanEnabledByProperties(log.debug,
      "org.catacombae.debug",
      "org.catacombae.io.debug",
      "org.catacombae.io." +
        BasicConcatenatedStream.class.getSimpleName() + ".debug");

    log.trace = Util.booleanEnabledByProperties(log.trace,
      "org.catacombae.debug",
      "org.catacombae.io.debug",
      "org.catacombae.io." +
        BasicConcatenatedStream.class.getSimpleName() + ".trace");
  }

  protected class Part {

    public final A file;
    public final long startOffset;
    public final long length;

    public Part(A file, long startOffset, long length) {
      this.file = file;
      this.startOffset = startOffset;
      this.length = length;
    }
  }

  protected final List<Part> parts = new ArrayList<Part>();
  protected long virtualFP;

  protected BasicConcatenatedStream(A firstPart, long startOffset, long length) {
    if (log.trace)
      log.traceEnter(firstPart, startOffset, length);

    try {
      Part currentPart = new Part(firstPart, startOffset, length);
      parts.add(currentPart);
      virtualFP = 0;
    } finally {
      if (log.trace)
        log.traceLeave(firstPart, startOffset, length);
    }
  }

  private void enter(String methodName, Object... args) {
    PrintStream out = System.err;
    out.print(this.getClass().getSimpleName() + "{" +
      this.hashCode() + "}");
    if (methodName != null)
      out.print("." + methodName);
    out.print("(");
    for (int i = 0; i < args.length; ++i) {
      if (i > 0)
        out.print(", ");
      out.print(args[i].toString());
    }
    out.println(");");
  }

  private void log(String methodName, String message) {
    PrintStream out = System.err;
    out.println(this.getClass().getSimpleName() + "{" +
      this.hashCode() + "}." + methodName + ": " + message);
  }

  public void addPart(A newFile, long off, long len) {
    if (log.trace)
      log.traceEnter(newFile, off, len);

    Part newPart = new Part(newFile, off, len);
    parts.add(newPart);

    if (log.trace)
      log.traceLeave(newFile, off, len);
  }

  @Override
  public void seek(long pos) {
    if (log.trace)
      log.traceEnter(pos);

    virtualFP = pos;

    if (log.trace)
      log.traceLeave(pos);
  }

  @Override
  public int read(byte[] data, int off, int len) {
    //String METHOD_NAME = "read";
    if (log.trace)
      log.traceEnter(data, off, len);

    if (log.debug) {
      log.debug("virtualFP=" + virtualFP);
    }

    try {
      int bytesRead = 0;

      long bytesToSkip = virtualFP;
      int requestedPartIndex = -1;
      for (Part p : parts) {
        ++requestedPartIndex;

        if (bytesToSkip > p.length) {
          bytesToSkip -= p.length;
        } else {
          break;
        }
      }
      if (requestedPartIndex == -1)
        return -1;

      while (requestedPartIndex < parts.size()) {
        Part requestedPart = parts.get(requestedPartIndex++);

        if (log.debug) {
          log.debug("requestedPart.length = " + requestedPart.length);
          log.debug("requestedPart.startOffset = " +
            requestedPart.startOffset);
        }

        long bytesToSkipInPart = bytesToSkip;

        if (log.debug) {
          log.debug("bytesToSkipInPart=" + bytesToSkipInPart);
        }

        bytesToSkip = 0;

        int bytesLeftToRead = len - bytesRead;

        if (log.debug) {
          log.debug("bytesLeftToRead = " + bytesLeftToRead);
        }

        int bytesToRead = (int) (bytesLeftToRead < requestedPart.length
          ? bytesLeftToRead : requestedPart.length);

        if (log.debug) {
          log.debug("bytesToRead = " + bytesToRead);
          log.debug("seeking to " + bytesToSkipInPart);
        }

        requestedPart.file.seek(requestedPart.startOffset + bytesToSkipInPart);

        if (log.debug) {
          log.debug("invoking requestedPart.file.read(byte[" +
            data.length + "], " + (off + bytesRead) + ", " +
            bytesToRead + ")");
        }

        int res = requestedPart.file.read(data, off + bytesRead,
          bytesToRead);

        if (log.debug) {
          log.debug("res = " + res);
        }

        if (res > 0) {
          virtualFP += res;
          bytesRead += res;
          if (bytesRead == len) {
            if (log.debug) {
              log.debug("returning " + bytesRead);
            }

            return bytesRead;
          } else if (bytesRead > len)
            throw new RuntimeException("Read more than I was " +
              "supposed to! This should not be possible.");
        } else {
          if (bytesRead > 0)
            return bytesRead;
          else
            return -1;
        }
      }

      if (log.trace)
        log.traceReturn(bytesRead);
      return bytesRead;
    } finally {
      if (log.trace)
        log.traceLeave(data, off, len);
    }
  }

  @Override
  public long length() {
    //String METHOD_NAME = "length";
    if (log.trace)
      log.traceEnter();

    long result = 0;
    for (Part p : parts)
      result += p.length;
    log.debug("returning " + result);

    if (log.trace) {
      log.traceReturn(virtualFP);
      log.traceLeave();
    }
    return result;
  }

  @Override
  public long getFilePointer() {
    //String METHOD_NAME = "getFilePointer";
    if (log.trace) {
      log.traceEnter();
      log.traceReturn(virtualFP);
      log.traceLeave();
    }
    return virtualFP;
  }

  /** Closes all the files constituting this BasicConcatenatedStream. */
  @Override
  public void close() {
    if (log.trace)
      log.traceEnter();

    for (Part p : parts)
      p.file.close();

    if (log.trace)
      log.traceLeave();
  }

}
