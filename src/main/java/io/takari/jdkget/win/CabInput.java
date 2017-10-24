package io.takari.jdkget.win;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public interface CabInput extends Closeable, ByteSource {

  /**
   * @throws IOException
   * @see InputStream#read()
   */
  int read() throws IOException;

  void seekRelative(long offset) throws IOException;

  void seekAbsolute(long offset) throws IOException;

  boolean negativeSeekSupported();

  long getOffset() throws IOException;

  long getLength() throws IOException;

  public static CabInput fromFile(RandomAccessFile file) {
    return new FileCabInput(file);
  }

  public static CabInput fromFile(File file) throws FileNotFoundException {
    return fromFile(new RandomAccessFile(file, "r"));
  }

  public static CabInput fromFile(String name) throws FileNotFoundException {
    return fromFile(new File(name));
  }

  public static CabInput fromStream(InputStream in) {
    return fromStream(in, 0);
  }

  public static CabInput fromStream(InputStream in, long currentOffset) {
    return new StreamCabInput(in, currentOffset);
  }

  static class FileCabInput implements CabInput {

    private RandomAccessFile file;

    public FileCabInput(RandomAccessFile file) {
      this.file = file;
    }

    @Override
    public void close() throws IOException {
      file.close();
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
      return file.read(buf, offset, len);
    }

    @Override
    public int read() throws IOException {
      return file.read();
    }

    @Override
    public void seekRelative(long offset) throws IOException {
      file.seek(file.getFilePointer() + offset);
    }

    @Override
    public void seekAbsolute(long offset) throws IOException {
      file.seek(offset);
    }

    @Override
    public boolean negativeSeekSupported() {
      return true;
    }

    @Override
    public long getOffset() throws IOException {
      return file.getFilePointer();
    }
    
    @Override
    public long getLength() throws IOException {
      return file.length();
    }
  }

  static class StreamCabInput implements CabInput {

    private InputStream in;
    private long currentOffset;

    public StreamCabInput(InputStream in, long currentOffset) {
      this.in = in;
      this.currentOffset = currentOffset;
    }

    @Override
    public void close() throws IOException {
      in.close();
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
      int l = in.read(buf, offset, len);
      if (l > 0) {
        currentOffset += l;
      }
      return l;
    }

    @Override
    public int read() throws IOException {
      int i = in.read();
      if (i >= 0) {
        currentOffset++;
      }
      return i;
    }

    @Override
    public void seekRelative(long offset) throws IOException {
      if (offset < 0) {
        throw new IllegalStateException("Stream input cannot seek backwards");
      }
      if (in.skip(offset) != offset) {
        throw new IOException("Error seeking");
      }
      currentOffset += offset;
    }

    @Override
    public void seekAbsolute(long offset) throws IOException {
      seekRelative(offset - currentOffset);
    }

    @Override
    public boolean negativeSeekSupported() {
      return false;
    }

    @Override
    public long getOffset() throws IOException {
      return currentOffset;
    }

    @Override
    public long getLength() throws IOException {
      return -1;
    }

  }
}
