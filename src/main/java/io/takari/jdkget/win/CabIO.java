package io.takari.jdkget.win;

public final class CabIO {
  private CabIO() {}

  static int EndGetI16(byte[] buf, int off) {
    return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);
  }

  static int EndGetI32(byte[] buf, int off) {
    return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8) | ((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
  }

  static long EndGetL32(byte[] buf, int off) {
    return ((long) buf[off] & 0xFF) | (((long) buf[off + 1] & 0xFF) << 8) | (((long) buf[off + 2] & 0xFF) << 16) | (((long) buf[off + 3] & 0xFF) << 24);
  }
}
