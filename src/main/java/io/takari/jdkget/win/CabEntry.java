package io.takari.jdkget.win;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.base.Throwables;

import io.takari.jdkget.win.CabDecompressor.Extractor;
import io.takari.jdkget.win.Cabinet.CabFile;

public class CabEntry {

  private final Cabinet cab;
  private final CabFile file;

  CabEntry(Cabinet cab, CabFile file) {
    this.cab = cab;
    this.file = file;
  }

  public String getName() {
    return file.filename;
  }

  public void extract(OutputStream out) throws IOException {
    try (CabDecompressor dec = new CabDecompressor()) {
      dec.cabd_extract(cab, file).extract(out);
    }
  }

  public InputStream getInputStream() throws IOException {
    CabDecompressor dec = new CabDecompressor();
    try {
      return new ExtractorInputStream(dec, dec.cabd_extract(cab, file), CabConstants.CAB_BLOCKMAX);
    } catch (Throwable e) {
      dec.close();
      Throwables.propagateIfInstanceOf(e, IOException.class);
      throw Throwables.propagate(e);
    }
  }

  private static class ExtractorInputStream extends InputStream {

    private CabDecompressor dec;
    private Extractor ex;

    private final int l;
    private Bout buf;
    private int avail;
    private boolean eof;

    public ExtractorInputStream(CabDecompressor dec, Extractor ex, int bufLength) {
      this.dec = dec;
      this.ex = ex;
      this.l = bufLength;
      avail = 0;
    }

    @Override
    public void close() throws IOException {
      dec.close();
    }

    @Override
    public int read() throws IOException {
      if (avail == 0) {
        readNext();
        if (eof && avail == 0) {
          avail = -1;
        }
      }
      if (avail == -1) {
        return -1;
      }
      return buf.buf[l - avail--];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }

      if (avail == -1) {
        return -1;
      }

      int todo = len;
      while (todo > 0) {
        if (avail == 0) {
          readNext();
        }

        int l = avail;
        if (l > todo) {
          l = todo;
        }

        if (l > 0) {
          System.arraycopy(buf.buf, this.l - avail, b, off, l);
          avail -= l;
          todo -= l;
          off += l;
        }

        if (eof && avail == 0) {
          avail = -1;
          break;
        }
      }
      return len - todo;
    }

    private void readNext() throws IOException {
      if (avail > 0 || eof) {
        return;
      }

      long req = ex.available();
      if (req > l) {
        req = l;
      }

      if (buf == null) {
        buf = new Bout(l);
      }

      buf.count = 0;
      ex.extract(buf, req);
      avail = (int) req;
      if (ex.available() == 0) {
        eof = true;
      }
    }

  }

  private static class Bout extends OutputStream {
    byte[] buf;
    int count;

    public Bout(int size) {
      this.buf = new byte[size];
      count = 0;
    }

    public void write(int b) {
      buf[count] = (byte) b;
      count += 1;
    }

    public void write(byte b[], int off, int len) {
      if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) - b.length > 0)) {
        throw new IndexOutOfBoundsException();
      }
      System.arraycopy(b, off, buf, count, len);
      count += len;
    }
  }

}
