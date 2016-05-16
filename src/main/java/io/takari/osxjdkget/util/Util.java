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

package io.takari.osxjdkget.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Util {
  public static class Pair<A, B> {
    private A a;
    private B b;

    public Pair() {
      a = null;
      b = null;
    }

    public Pair(A iA, B iB) {
      a = iA;
      b = iB;
    }

    public final A getA() {
      return a;
    }

    public final B getB() {
      return b;
    }

    public final void setA(A iA) {
      a = iA;
    }

    public final void setB(B iB) {
      b = iB;
    }
  }

  public static String byteArrayToHexString(byte[] array) {
    return byteArrayToHexString(array, 0, array.length);
  }

  public static String byteArrayToHexString(byte[] array, int offset,
    int length) {
    String result = "";
    for (int i = offset; i < (offset + length); ++i) {
      byte b = array[i];
      String currentHexString = Integer.toHexString(b & 0xFF);
      if (currentHexString.length() == 1)
        currentHexString = "0" + currentHexString;
      result += currentHexString;
    }
    return result;
  }

  public static String toHexStringBE(char[] array) {
    return toHexStringBE(array, 0, array.length);
  }

  public static String toHexStringBE(char[] array, int offset, int length) {
    StringBuilder result = new StringBuilder();
    for (int i = offset; i < length; ++i)
      result.append(toHexStringBE(array[i]));
    return result.toString();
  }

  public static String toHexStringBE(short[] array) {
    return toHexStringBE(array, 0, array.length);
  }

  public static String toHexStringBE(short[] array, int offset, int length) {
    StringBuilder result = new StringBuilder();
    for (int i = offset; i < length; ++i)
      result.append(toHexStringBE(array[i]));
    return result.toString();
  }

  public static String toHexStringBE(int[] array) {
    return toHexStringBE(array, 0, array.length);
  }

  public static String toHexStringBE(int[] array, int offset, int length) {
    StringBuilder result = new StringBuilder();
    for (int i = offset; i < length; ++i)
      result.append(toHexStringBE(array[i]));
    return result.toString();
  }

  public static String toHexStringLE(byte n) {
    return byteArrayToHexString(toByteArrayLE(n));
  }

  public static String toHexStringLE(short n) {
    return byteArrayToHexString(toByteArrayLE(n));
  }

  public static String toHexStringLE(char n) {
    return byteArrayToHexString(toByteArrayLE(n));
  }

  public static String toHexStringLE(int n) {
    return byteArrayToHexString(toByteArrayLE(n));
  }

  public static String toHexStringLE(long n) {
    return byteArrayToHexString(toByteArrayLE(n));
  }

  public static String toHexStringBE(byte n) {
    return byteArrayToHexString(toByteArrayBE(n));
  }

  public static String toHexStringBE(short n) {
    return byteArrayToHexString(toByteArrayBE(n));
  }

  public static String toHexStringBE(char n) {
    return byteArrayToHexString(toByteArrayBE(n));
  }

  public static String toHexStringBE(int n) {
    return byteArrayToHexString(toByteArrayBE(n));
  }

  public static String toHexStringBE(long n) {
    return byteArrayToHexString(toByteArrayBE(n));
  }

  public static byte[] invert(byte[] array) {
    byte[] newArray = new byte[array.length];
    for (int i = 0; i < array.length; ++i)
      newArray[newArray.length - i - 1] = array[i];
    return newArray;
  }

  public static long readLongLE(byte[] data) {
    return readLongLE(data, 0);
  }

  public static long readLongLE(byte[] data, int offset) {
    return (((long) data[offset + 7] & 0xFF) << 56 |
      ((long) data[offset + 6] & 0xFF) << 48 |
      ((long) data[offset + 5] & 0xFF) << 40 |
      ((long) data[offset + 4] & 0xFF) << 32 |
      ((long) data[offset + 3] & 0xFF) << 24 |
      ((long) data[offset + 2] & 0xFF) << 16 |
      ((long) data[offset + 1] & 0xFF) << 8 |
      ((long) data[offset + 0] & 0xFF) << 0);
  }

  public static int readIntLE(byte[] data) {
    return readIntLE(data, 0);
  }

  public static int readIntLE(byte[] data, int offset) {
    return ((data[offset + 3] & 0xFF) << 24 |
      (data[offset + 2] & 0xFF) << 16 |
      (data[offset + 1] & 0xFF) << 8 |
      (data[offset + 0] & 0xFF) << 0);
  }

  public static short readShortLE(byte[] data) {
    return readShortLE(data, 0);
  }

  public static short readShortLE(byte[] data, int offset) {
    return (short) ((data[offset + 1] & 0xFF) << 8 |
      (data[offset + 0] & 0xFF) << 0);
  }

  public static byte readByteLE(byte[] data) {
    return readByteLE(data, 0);
  }

  public static byte readByteLE(byte[] data, int offset) {
    return data[offset];
  }

  public static long readLongBE(byte[] data) {
    return readLongBE(data, 0);
  }

  public static long readLongBE(byte[] data, int offset) {
    return (((long) data[offset + 0] & 0xFF) << 56 |
      ((long) data[offset + 1] & 0xFF) << 48 |
      ((long) data[offset + 2] & 0xFF) << 40 |
      ((long) data[offset + 3] & 0xFF) << 32 |
      ((long) data[offset + 4] & 0xFF) << 24 |
      ((long) data[offset + 5] & 0xFF) << 16 |
      ((long) data[offset + 6] & 0xFF) << 8 |
      ((long) data[offset + 7] & 0xFF) << 0);
  }

  public static int readIntBE(byte[] data) {
    return readIntBE(data, 0);
  }

  public static int readIntBE(byte[] data, int offset) {
    return ((data[offset + 0] & 0xFF) << 24 |
      (data[offset + 1] & 0xFF) << 16 |
      (data[offset + 2] & 0xFF) << 8 |
      (data[offset + 3] & 0xFF) << 0);
  }

  public static short readShortBE(byte[] data) {
    return readShortBE(data, 0);
  }

  public static short readShortBE(byte[] data, int offset) {
    return (short) ((data[offset + 0] & 0xFF) << 8 |
      (data[offset + 1] & 0xFF) << 0);
  }

  public static byte readByteBE(byte[] data) {
    return readByteBE(data, 0);
  }

  public static byte readByteBE(byte[] data, int offset) {
    return data[offset];
  }

  public static byte[] toByteArrayLE(byte b) {
    byte[] result = new byte[1];
    result[0] = b;
    return result;
  }

  public static byte[] toByteArrayLE(short s) {
    byte[] result = new byte[2];
    arrayPutLE(result, 0, s);
    return result;
  }

  public static byte[] toByteArrayLE(char c) {
    byte[] result = new byte[2];
    arrayPutLE(result, 0, c);
    return result;
  }

  public static byte[] toByteArrayLE(int i) {
    byte[] result = new byte[4];
    arrayPutLE(result, 0, i);
    return result;
  }

  public static byte[] toByteArrayLE(long l) {
    byte[] result = new byte[8];
    arrayPutLE(result, 0, l);
    return result;
  }

  public static byte[] toByteArrayBE(byte b) {
    byte[] result = new byte[1];
    result[0] = b;
    return result;
  }

  public static byte[] toByteArrayBE(short s) {
    byte[] result = new byte[2];
    arrayPutBE(result, 0, s);
    return result;
  }

  public static byte[] toByteArrayBE(char c) {
    byte[] result = new byte[2];
    arrayPutBE(result, 0, c);
    return result;
  }

  public static byte[] toByteArrayBE(int i) {
    byte[] result = new byte[4];
    arrayPutBE(result, 0, i);
    return result;
  }

  public static byte[] toByteArrayBE(long l) {
    byte[] result = new byte[8];
    arrayPutBE(result, 0, l);
    return result;
  }

  public static boolean zeroed(byte[] ba) {
    for (byte b : ba)
      if (b != 0)
        return false;
    return true;
  }

  public static void zero(byte[]... arrays) {
    for (byte[] ba : arrays)
      set(ba, 0, ba.length, (byte) 0);
  }

  public static void zero(byte[] ba, int offset, int length) {
    set(ba, offset, length, (byte) 0);
  }

  public static void zero(short[]... arrays) {
    for (short[] array : arrays)
      set(array, 0, array.length, (short) 0);
  }

  public static void zero(short[] ba, int offset, int length) {
    set(ba, offset, length, (short) 0);
  }

  public static void zero(int[]... arrays) {
    for (int[] array : arrays)
      set(array, 0, array.length, 0);
  }

  public static void zero(int[] ba, int offset, int length) {
    set(ba, offset, length, 0);
  }

  public static void zero(long[]... arrays) {
    for (long[] array : arrays)
      set(array, 0, array.length, 0);
  }

  public static void zero(long[] ba, int offset, int length) {
    set(ba, offset, length, 0);
  }

  public static void set(boolean[] array, boolean value) {
    set(array, 0, array.length, value);
  }

  public static void set(byte[] array, byte value) {
    set(array, 0, array.length, value);
  }

  public static void set(short[] array, short value) {
    set(array, 0, array.length, value);
  }

  public static void set(char[] array, char value) {
    set(array, 0, array.length, value);
  }

  public static void set(int[] array, int value) {
    set(array, 0, array.length, value);
  }

  public static void set(long[] array, long value) {
    set(array, 0, array.length, value);
  }

  public static <T> void set(T[] array, T value) {
    set(array, 0, array.length, value);
  }

  public static void set(boolean[] ba, int offset, int length, boolean value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static void set(byte[] ba, int offset, int length, byte value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static void set(short[] ba, int offset, int length, short value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static void set(char[] ba, int offset, int length, char value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static void set(int[] ba, int offset, int length, int value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static void set(long[] ba, int offset, int length, long value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static <T> void set(T[] ba, int offset, int length, T value) {
    for (int i = offset; i < length; ++i)
      ba[i] = value;
  }

  public static byte[] createCopy(byte[] data) {
    return createCopy(data, 0, data.length);
  }

  public static byte[] createCopy(byte[] data, int offset, int length) {
    byte[] copy = new byte[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  public static char[] createCopy(char[] data) {
    return createCopy(data, 0, data.length);
  }

  public static char[] createCopy(char[] data, int offset, int length) {
    char[] copy = new char[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  public static short[] createCopy(short[] data) {
    return createCopy(data, 0, data.length);
  }

  public static short[] createCopy(short[] data, int offset, int length) {
    short[] copy = new short[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  public static int[] createCopy(int[] data) {
    return createCopy(data, 0, data.length);
  }

  public static int[] createCopy(int[] data, int offset, int length) {
    int[] copy = new int[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  public static long[] createCopy(long[] data) {
    return createCopy(data, 0, data.length);
  }

  public static long[] createCopy(long[] data, int offset, int length) {
    long[] copy = new long[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  /**
   * Creates a copy of the input data reversed byte by byte. This is helpful
   * for endian swapping.
   *
   * @param data
   * @return a copy of the input data reversed byte by byte.
   */
  public static byte[] createReverseCopy(byte[] data) {
    return createReverseCopy(data, 0, data.length);
  }

  /**
   * Creates a copy of the input data reversed byte by byte. This is helpful
   * for endian swapping.
   *
   * @param data
   * @param offset
   * @param length
   * @return a copy of the input data reversed byte by byte.
   */
  public static byte[] createReverseCopy(byte[] data, int offset, int length) {
    byte[] copy = new byte[length];
    for (int i = 0; i < copy.length; ++i) {
      copy[i] = data[offset + (length - i - 1)];
    }
    return copy;
  }

  public static byte[] arrayCopy(byte[] source, byte[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static byte[] arrayCopy(byte[] source, int sourcePos, byte[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static byte[] arrayCopy(byte[] source, byte[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static byte[] arrayCopy(byte[] source, int sourcePos, byte[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static byte[] arrayCopy(byte[] source, int sourcePos, byte[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static boolean[] arrayCopy(boolean[] source, boolean[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static boolean[] arrayCopy(boolean[] source, int sourcePos,
    boolean[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static boolean[] arrayCopy(boolean[] source, boolean[] dest,
    int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static boolean[] arrayCopy(boolean[] source, int sourcePos,
    boolean[] dest, int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static boolean[] arrayCopy(boolean[] source, int sourcePos,
    boolean[] dest, int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static short[] arrayCopy(short[] source, short[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static short[] arrayCopy(short[] source, int sourcePos, short[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static short[] arrayCopy(short[] source, short[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static short[] arrayCopy(short[] source, int sourcePos, short[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static short[] arrayCopy(short[] source, int sourcePos, short[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static char[] arrayCopy(char[] source, char[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static char[] arrayCopy(char[] source, int sourcePos, char[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static char[] arrayCopy(char[] source, char[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static char[] arrayCopy(char[] source, int sourcePos, char[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static char[] arrayCopy(char[] source, int sourcePos, char[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static int[] arrayCopy(int[] source, int[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static int[] arrayCopy(int[] source, int sourcePos, int[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static int[] arrayCopy(int[] source, int[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static int[] arrayCopy(int[] source, int sourcePos, int[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static int[] arrayCopy(int[] source, int sourcePos, int[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static long[] arrayCopy(long[] source, long[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static long[] arrayCopy(long[] source, int sourcePos, long[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static long[] arrayCopy(long[] source, long[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static long[] arrayCopy(long[] source, int sourcePos, long[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static long[] arrayCopy(long[] source, int sourcePos, long[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length) {
      throw new RuntimeException("Source array not large enough.");
    }

    if (dest.length - destPos < length) {
      throw new RuntimeException("Destination array not large enough.");
    }

    System.arraycopy(source, sourcePos, dest, destPos, length);

    return dest;
  }

  public static <T> T[] arrayCopy(T[] source, T[] dest) {
    return arrayCopy(source, 0, dest, 0);
  }

  public static <T> T[] arrayCopy(T[] source, int sourcePos, T[] dest) {
    return arrayCopy(source, sourcePos, dest, 0);
  }

  public static <T> T[] arrayCopy(T[] source, T[] dest, int destPos) {
    return arrayCopy(source, 0, dest, destPos);
  }

  public static <T> T[] arrayCopy(T[] source, int sourcePos, T[] dest,
    int destPos) {
    return arrayCopy(source, sourcePos, dest, destPos,
      source.length - sourcePos);
  }

  public static <T> T[] arrayCopy(T[] source, int sourcePos, T[] dest,
    int destPos, int length) {
    if (source.length - sourcePos < length)
      throw new RuntimeException("Source array not large enough.");
    if (dest.length - destPos < length)
      throw new RuntimeException("Destination array not large enough.");
    System.arraycopy(source, sourcePos, dest, destPos, length);
    return dest;
  }

  public static boolean arraysEqual(boolean[] a, boolean[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(boolean[] a, int aoff, int alen,
    boolean[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(byte[] a, byte[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(byte[] a, int aoff, int alen,
    byte[] b, int boff, int blen) {
    if (a.length != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(char[] a, char[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(char[] a, int aoff, int alen,
    char[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(short[] a, short[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(short[] a, int aoff, int alen,
    short[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(int[] a, int[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(int[] a, int aoff, int alen,
    int[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(long[] a, long[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(long[] a, int aoff, int alen,
    long[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (a[aoff + i] != b[boff + i])
          return false;
      return true;
    }
  }

  public static boolean arraysEqual(Object[] a, Object[] b) {
    return arrayRegionsEqual(a, 0, a.length, b, 0, b.length);
  }

  public static boolean arrayRegionsEqual(Object[] a, int aoff, int alen,
    Object[] b, int boff, int blen) {
    if (alen != blen)
      return false;
    else {
      for (int i = 0; i < alen; ++i)
        if (!a[aoff + i].equals(b[boff + i]))
          return false;
      return true;
    }
  }

  public static long pow(long a, long b) {
    if (b < 0)
      throw new IllegalArgumentException("b can not be negative");

    long result = 1;
    for (long i = 0; i < b; ++i)
      result *= a;
    return result;
  }

  public static int strlen(byte[] data) {
    int length = 0;
    for (byte b : data) {
      if (b == 0)
        break;
      else
        ++length;
    }
    return length;
  }

  public static boolean getBit(long data, int bitNumber) {
    return ((data >>> bitNumber) & 0x1) == 0x1;
  }

  public static byte setBit(byte data, int bitNumber, boolean value) {
    if (bitNumber < 0 || bitNumber > 7)
      throw new IllegalArgumentException("bitNumber out of range");
    return (byte) setBit(data & 0xFF, bitNumber, value);
  }

  public static short setBit(short data, int bitNumber, boolean value) {
    if (bitNumber < 0 || bitNumber > 15)
      throw new IllegalArgumentException("bitNumber out of range");
    return (short) setBit(data & 0xFFFF, bitNumber, value);
  }

  public static char setBit(char data, int bitNumber, boolean value) {
    if (bitNumber < 0 || bitNumber > 15)
      throw new IllegalArgumentException("bitNumber out of range");
    return (char) setBit(data & 0xFFFF, bitNumber, value);
  }

  public static int setBit(int data, int bitNumber, boolean value) {
    if (bitNumber < 0 || bitNumber > 31)
      throw new IllegalArgumentException("bitNumber out of range");
    if (value)
      return data | (0x1 << bitNumber);
    else
      return data & (data ^ (0x1 << bitNumber));
  }

  public static long setBit(long data, int bitNumber, boolean value) {
    if (bitNumber < 0 || bitNumber > 63)
      throw new IllegalArgumentException("bitNumber out of range");
    if (value)
      return data | (0x1 << bitNumber);
    else
      return data & (data ^ (0x1 << bitNumber));
  }

  public static int arrayCompareLex(byte[] a, byte[] b) {
    return arrayCompareLex(a, 0, a.length, b, 0, b.length);
  }

  public static int arrayCompareLex(byte[] a, int aoff, int alen, byte[] b,
    int boff, int blen) {
    int compareLen = alen < blen ? alen : blen; // equiv. Math.min
    for (int i = 0; i < compareLen; ++i) {
      byte curA = a[aoff + i];
      byte curB = b[boff + i];
      if (curA != curB)
        return curA - curB;
    }
    return alen - blen; // The shortest array gets higher priority
  }

  public static int unsignedArrayCompareLex(byte[] a, byte[] b) {
    return unsignedArrayCompareLex(a, 0, a.length, b, 0, b.length);
  }

  public static int unsignedArrayCompareLex(byte[] a, int aoff, int alen,
    byte[] b, int boff, int blen) {
    int compareLen = alen < blen ? alen : blen; // equiv. Math.min
    for (int i = 0; i < compareLen; ++i) {
      int curA = a[aoff + i] & 0xFF;
      int curB = b[boff + i] & 0xFF;
      if (curA != curB)
        return curA - curB;
    }
    return alen - blen; // The shortest array gets higher priority
  }

  public static int unsignedArrayCompareLex(char[] a, char[] b) {
    return unsignedArrayCompareLex(a, 0, a.length, b, 0, b.length);
  }

  public static int unsignedArrayCompareLex(char[] a, int aoff, int alen,
    char[] b, int boff, int blen) {
    int compareLen = alen < blen ? alen : blen; // equiv. Math.min
    for (int i = 0; i < compareLen; ++i) {
      int curA = a[aoff + i] & 0xFFFF; // Unsigned char values represented as int
      int curB = b[boff + i] & 0xFFFF;
      if (curA != curB)
        return curA - curB;
    }
    return alen - blen; // The shortest array gets higher priority
  }

  // All below is from Util2 (got tired of having two Util classes...)
  public static String toASCIIString(byte[] data) {
    return toASCIIString(data, 0, data.length);
  }

  public static String toASCIIString(byte[] data, int offset, int length) {
    return readString(data, offset, length, "US-ASCII");
  }

  public static String toASCIIString(short i) {
    return toASCIIString(Util.toByteArrayBE(i));
  }

  public static String toASCIIString(int i) {
    return toASCIIString(Util.toByteArrayBE(i));
  }

  public static String readString(byte[] data, String encoding) {
    return readString(data, 0, data.length, encoding);
  }

  public static String readString(byte[] data, int offset, int length,
    String encoding) {
    try {
      return new String(data, offset, length, encoding);
    } catch (Exception e) {
      return null;
    }
  }

  public static String readString(short i, String encoding) {
    return readString(Util.toByteArrayBE(i), encoding);
  }

  public static String readString(int i, String encoding) {
    return readString(Util.toByteArrayBE(i), encoding);
  }

  public static String readNullTerminatedASCIIString(byte[] data) {
    return readNullTerminatedASCIIString(data, 0, data.length);
  }

  public static String readNullTerminatedASCIIString(byte[] data, int offset,
    int maxLength) {
    int i;
    for (i = offset; i < (offset + maxLength); ++i)
      if (data[i] == 0)
        break;
    return toASCIIString(data, offset, i - offset);
  }

  public static char readCharLE(byte[] data) {
    return readCharLE(data, 0);
  }

  public static char readCharLE(byte[] data, int offset) {
    return (char) ((data[offset + 1] & 0xFF) << 8 |
      (data[offset + 0] & 0xFF) << 0);
  }

  public static char readCharBE(byte[] data) {
    return readCharBE(data, 0);
  }

  public static char readCharBE(byte[] data, int offset) {
    return (char) ((data[offset + 0] & 0xFF) << 8 |
      (data[offset + 1] & 0xFF) << 0);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(byte b) {
    return readByteArrayBE(toByteArrayBE(b), 0, 1 * 1);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(short s) {
    return readByteArrayBE(toByteArrayBE(s), 0, 2 * 1);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(char c) {
    return readByteArrayBE(toByteArrayBE(c), 0, 2 * 1);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(int i) {
    return readByteArrayBE(toByteArrayBE(i), 0, 4 * 1);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(long l) {
    return readByteArrayBE(toByteArrayBE(l), 0, 8 * 1);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(byte[] b) {
    return readByteArrayBE(b, 0, b.length);
  }

  /** Stupid method which should go away. */
  public static byte[] readByteArrayBE(byte[] b, int offset, int size) {
    return createCopy(b, offset, size);
  }

  public static char[] readCharArrayLE(short s) {
    return readCharArrayLE(toByteArrayBE(s), 0, 1 * 2);
  }

  public static char[] readCharArrayLE(char c) {
    return readCharArrayLE(toByteArrayBE(c), 0, 1 * 2);
  }

  public static char[] readCharArrayLE(int i) {
    return readCharArrayLE(toByteArrayBE(i), 0, 2 * 2);
  }

  public static char[] readCharArrayLE(long l) {
    return readCharArrayLE(toByteArrayBE(l), 0, 4 * 2);
  }

  public static char[] readCharArrayLE(byte[] b) {
    return readCharArrayLE(b, 0, b.length);
  }

  public static char[] readCharArrayLE(byte[] b, int offset, int length) {
    char[] result = new char[length / 2];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readCharLE(b, offset + (i * 2));
    return result;
  }

  public static char[] readCharArrayBE(short s) {
    return readCharArrayBE(toByteArrayBE(s), 0, 1 * 2);
  }

  public static char[] readCharArrayBE(char c) {
    return readCharArrayBE(toByteArrayBE(c), 0, 1 * 2);
  }

  public static char[] readCharArrayBE(int i) {
    return readCharArrayBE(toByteArrayBE(i), 0, 2 * 2);
  }

  public static char[] readCharArrayBE(long l) {
    return readCharArrayBE(toByteArrayBE(l), 0, 4 * 2);
  }

  public static char[] readCharArrayBE(byte[] b) {
    return readCharArrayBE(b, 0, b.length);
  }

  public static char[] readCharArrayBE(byte[] b, int offset, int length) {
    char[] result = new char[length / 2];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readCharBE(b, offset + (i * 2));
    return result;
  }

  public static short[] readShortArrayLE(short s) {
    return readShortArrayLE(toByteArrayBE(s), 0, 1 * 2);
  }

  public static short[] readShortArrayLE(char c) {
    return readShortArrayLE(toByteArrayBE(c), 0, 1 * 2);
  }

  public static short[] readShortArrayLE(int i) {
    return readShortArrayLE(toByteArrayBE(i), 0, 2 * 2);
  }

  public static short[] readShortArrayLE(long l) {
    return readShortArrayLE(toByteArrayBE(l), 0, 4 * 2);
  }

  public static short[] readShortArrayLE(byte[] b) {
    return readShortArrayLE(b, 0, b.length);
  }

  public static short[] readShortArrayLE(byte[] b, int offset, int length) {
    short[] result = new short[length / 2];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readShortLE(b, offset + (i * 2));
    return result;
  }

  public static short[] readShortArrayBE(short s) {
    return readShortArrayBE(toByteArrayBE(s), 0, 1 * 2);
  }

  public static short[] readShortArrayBE(char c) {
    return readShortArrayBE(toByteArrayBE(c), 0, 1 * 2);
  }

  public static short[] readShortArrayBE(int i) {
    return readShortArrayBE(toByteArrayBE(i), 0, 2 * 2);
  }

  public static short[] readShortArrayBE(long l) {
    return readShortArrayBE(toByteArrayBE(l), 0, 4 * 2);
  }

  public static short[] readShortArrayBE(byte[] b) {
    return readShortArrayBE(b, 0, b.length);
  }

  public static short[] readShortArrayBE(byte[] b, int offset, int length) {
    short[] result = new short[length / 2];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readShortBE(b, offset + (i * 2));
    return result;
  }

  public static int[] readIntArrayLE(int i) {
    return readIntArrayLE(toByteArrayBE(i), 0, 1 * 4);
  }

  public static int[] readIntArrayLE(long l) {
    return readIntArrayLE(toByteArrayBE(l), 0, 2 * 4);
  }

  public static int[] readIntArrayLE(byte[] b) {
    return readIntArrayLE(b, 0, b.length);
  }

  public static int[] readIntArrayLE(byte[] b, int offset, int length) {
    int[] result = new int[length / 4];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readIntLE(b, offset + (i * 4));
    return result;
  }

  public static int[] readIntArrayBE(int i) {
    return readIntArrayBE(toByteArrayBE(i), 0, 1 * 4);
  }

  public static int[] readIntArrayBE(long l) {
    return readIntArrayBE(toByteArrayBE(l), 0, 2 * 4);
  }

  public static int[] readIntArrayBE(byte[] b) {
    return readIntArrayBE(b, 0, b.length);
  }

  public static int[] readIntArrayBE(byte[] b, int offset, int length) {
    int[] result = new int[length / 4];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readIntBE(b, offset + (i * 4));
    return result;
  }

  public static long[] readLongArrayLE(long l) {
    return readLongArrayLE(toByteArrayBE(l), 0, 1 * 8);
  }

  public static long[] readLongArrayLE(byte[] b) {
    return readLongArrayLE(b, 0, b.length);
  }

  public static long[] readLongArrayLE(byte[] b, int offset, int length) {
    long[] result = new long[length / 8];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readLongLE(b, offset + (i * 8));
    return result;
  }

  public static long[] readLongArrayBE(long l) {
    return readLongArrayBE(toByteArrayBE(l), 0, 1 * 8);
  }

  public static long[] readLongArrayBE(byte[] b) {
    return readLongArrayBE(b, 0, b.length);
  }

  public static long[] readLongArrayBE(byte[] b, int offset, int length) {
    long[] result = new long[length / 8];
    for (int i = 0; i < result.length; ++i)
      result[i] = Util.readLongBE(b, offset + (i * 8));
    return result;
  }

  public static byte[] readByteArrayLE(char[] data) {
    return readByteArrayLE(data, 0, data.length);
  }

  public static byte[] readByteArrayLE(char[] data, int offset, int length) {
    byte[] result = new byte[length * 2];
    for (int i = 0; i < length; ++i) {
      byte[] cur = toByteArrayLE(data[offset + i]);
      result[i * 2] = cur[0];
      result[i * 2 + 1] = cur[1];
    }
    return result;
  }

  public static byte[] readByteArrayBE(char[] data) {
    return readByteArrayBE(data, 0, data.length);
  }

  public static byte[] readByteArrayBE(char[] data, int offset, int length) {
    byte[] result = new byte[length * 2];
    for (int i = 0; i < length; ++i) {
      byte[] cur = toByteArrayBE(data[offset + i]);
      result[i * 2] = cur[0];
      result[i * 2 + 1] = cur[1];
    }
    return result;
  }

  public static byte[] fillBuffer(InputStream is, byte[] buffer)
    throws IOException {
    DataInputStream dis = new DataInputStream(is);
    dis.readFully(buffer);
    return buffer;
  }

  public static short unsign(byte b) {
    return (short) (b & 0xFF);
  }

  public static int unsign(short s) {
    return s & 0xFFFF;
  }

  public static int unsign(char s) {
    return s & 0xFFFF;
  }

  public static long unsign(int i) {
    return i & 0xFFFFFFFFL;
  }

  public static BigInteger unsign(long l) {
    return new BigInteger(1, toByteArrayBE(l));
  }

  public static short[] unsign(byte[] ab) {
    short[] res = new short[ab.length];
    for (int i = 0; i < ab.length; ++i)
      res[i] = unsign(ab[i]);
    return res;
  }

  public static int[] unsign(short[] as) {
    int[] res = new int[as.length];
    for (int i = 0; i < as.length; ++i)
      res[i] = unsign(as[i]);
    return res;
  }

  public static int[] unsign(char[] ac) {
    int[] res = new int[ac.length];
    for (int i = 0; i < ac.length; ++i)
      res[i] = unsign(ac[i]);
    return res;
  }

  public static long[] unsign(int[] ai) {
    long[] res = new long[ai.length];
    for (int i = 0; i < ai.length; ++i)
      res[i] = unsign(ai[i]);
    return res;
  }

  public static BigInteger[] unsign(long[] al) {
    BigInteger[] res = new BigInteger[al.length];
    for (int i = 0; i < al.length; ++i)
      res[i] = unsign(al[i]);
    return res;
  }

  // Added 2007-06-24 for DMGExtractor
  public static String readFully(Reader r) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] temp = new char[512];
    long bytesRead = 0;
    int curBytesRead = r.read(temp, 0, temp.length);
    while (curBytesRead >= 0) {
      sb.append(temp, 0, curBytesRead);
      curBytesRead = r.read(temp, 0, temp.length);
    }
    return sb.toString();
  }

  // Added 2007-06-26 for DMGExtractor
  public static String[] concatenate(String[] a, String... b) {
    String[] c = new String[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  public static <T> T[] concatenate(T[] a, T[] b, T[] target) {
    System.arraycopy(a, 0, target, 0, a.length);
    System.arraycopy(b, 0, target, a.length, b.length);
    return target;
  }

  // From IRCForME
  public static byte[] encodeString(String string, String encoding) {
    try {
      return string.getBytes(encoding);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Encodes a String containing only ASCII characters into an ASCII-encoded
   * byte array.
   *
   * @param s source string.
   * @return the ASCII-encoded byte string corresponding to <code>s</code>.
   */
  public static byte[] encodeASCIIString(String s) {
    byte[] result = new byte[s.codePointCount(0, s.length())];
    encodeASCIIString(s, 0, result, 0, result.length);
    return result;
  }

  /**
   * Encodes a String containing only ASCII characters into ASCII-encoded
   * data, stored in <code>b</code>.
   *
   * @param s     source string.
   * @param sPos  read position in source String.
   * @param b     target array.
   * @param bPos  store position in target array.
   * @param len   the number of codepoints to read from <code>s</code> and
   *              thus the number of bytes to write to <code>b</code>.
   */
  public static void encodeASCIIString(String s, int sPos, byte[] b, int bPos,
    final int len) {
    for (int i = 0; i < len; ++i) {
      int curCodePoint = s.codePointAt(i + sPos);

      if (curCodePoint >= 0 && curCodePoint < 0x80) {
        b[i + bPos] = (byte) curCodePoint;
      } else {
        throw new IllegalArgumentException("Illegal ASCII character: " +
          "\"" + new String(new int[] {curCodePoint}, 0, 1) +
          "\" (0x" + Util.toHexStringBE(curCodePoint) + ")");
      }
    }
  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static boolean contains(byte[] array, byte element) {
    for (byte b : array) {
      if (b == element)
        return true;
    }
    return false;

  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static boolean contains(char[] array, char element) {
    for (char c : array) {
      if (c == element)
        return true;
    }
    return false;

  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static boolean contains(short[] array, short element) {
    for (short s : array) {
      if (s == element)
        return true;
    }
    return false;

  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static boolean contains(int[] array, int element) {
    for (int i : array) {
      if (i == element)
        return true;
    }
    return false;

  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static boolean contains(long[] array, long element) {
    for (long l : array) {
      if (l == element)
        return true;
    }
    return false;

  }

  /**
   * Checks if the given <code>array</code> contains the specified
   * <code>element</code> at least once.
   *
   * @param array the array to search.
   * @param element the element to look for.
   * @return true if <code>element</code> was present in <code>array</code>,
   * and false otherwise.
   */
  public static <A> boolean contains(A[] array, A element) {
    for (A a : array) {
      if (a == element)
        return true;
    }
    return false;
  }

  /**
   * Checks if the given list of arrays contains an array that is equal to
   * <code>array</code> by the definition of Arrays.equal(..) (both arrays
   * must have the same number of elements, and every pair of elements must be
   * equal according to Object.equals).
   *
   * @param <A> the type of the array.
   * @param listOfArrays the list of arrays to search.
   * @param array the array to match.
   * @return <code>true</code> if an equal to <code>array</code> was found in
   * <code>listOfArrays</code>, otherwise <code>false</code>.
   */
  public static <A> boolean contains(List<A[]> listOfArrays, A[] array) {
    for (A[] curArray : listOfArrays) {
      if (Arrays.equals(curArray, array))
        return true;
    }
    return false;
  }

  /**
   * Concatenates the <code>strings</code> into one big string, putting
   * <code>glueString</code> between each pair. Example:<br/>
   * <code>concatenateStrings(new String[] {"joe", "lisa", "bob"},
   * " and ");</code> yields the string "joe and lisa and bob".
   *
   * @param strings
   * @param glueString
   * @return the input strings concatenated into one string, adding the
   * <code>glueString</code> between each pair.
   */
  public static String concatenateStrings(Object[] strings, String glueString) {
    if (strings.length > 0) {
      StringBuilder sb = new StringBuilder(strings[0].toString());
      for (int i = 1; i < strings.length; ++i)
        sb.append(glueString).append(strings[i].toString());
      return sb.toString();
    } else
      return "";
  }

  public static String concatenateStrings(List<? extends Object> strings,
    String glueString) {
    if (strings.size() > 0) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (Object s : strings) {
        if (!first)
          sb.append(glueString);
        else
          first = false;
        sb.append(s.toString());
      }
      return sb.toString();
    } else
      return "";
  }

  public static String addUnitSpaces(String string, int unitSize) {
    int parts = string.length() / unitSize;
    StringBuilder sizeStringBuilder = new StringBuilder();
    String head = string.substring(0, string.length() - parts * unitSize);
    if (head.length() > 0)
      sizeStringBuilder.append(head);
    for (int i = parts - 1; i >= 0; --i) {
      if (i < parts - 1 || (i == parts - 1 && head.length() > 0))
        sizeStringBuilder.append(" ");
      sizeStringBuilder.append(string.substring(string.length() -
        (i + 1) * unitSize, string.length() - i * unitSize));
    }
    return sizeStringBuilder.toString();
  }

  public static void buildStackTrace(Throwable t, int maxStackTraceLines,
    StringBuilder sb) {
    int stackTraceLineCount = 0;
    Throwable curThrowable = t;
    while (curThrowable != null && stackTraceLineCount < maxStackTraceLines) {
      sb.append(curThrowable.toString()).append("\n");
      ++stackTraceLineCount;
      for (StackTraceElement ste : curThrowable.getStackTrace()) {
        if (stackTraceLineCount < maxStackTraceLines) {
          sb.append("        ").append(ste.toString()).append("\n");
        }
        ++stackTraceLineCount;
      }

      Throwable cause = curThrowable.getCause();
      if (cause != null) {
        if (stackTraceLineCount < maxStackTraceLines) {
          sb.append("Caused by:\n");
          ++stackTraceLineCount;
        }
      }
      curThrowable = cause;
    }

    if (stackTraceLineCount >= maxStackTraceLines)
      sb.append("...and ").append(stackTraceLineCount - maxStackTraceLines).append(" more.");
  }

  /**
   * Reverses the order of the array <code>data</code>.
   *
   * @param data the array to reverse.
   * @return <code>data</code>.
   */
  public static byte[] byteSwap(byte[] data) {
    return byteSwap(data, 0, data.length);
  }

  /**
   * Reverses the order of the range defined by <code>offset</code> and
   * <code>length</code> in the array <code>data</code>.
   *
   * @param data the array to reverse.
   * @param offset the start offset of the region to reverse.
   * @param length the length of the region to reverse.
   * @return <code>data</code>.
   */
  public static byte[] byteSwap(byte[] data, int offset, int length) {
    int endOffset = offset + length - 1;
    int middleOffset = offset + (length / 2);
    byte tmp;

    for (int head = offset; head < middleOffset; ++head) {
      int tail = endOffset - head;
      if (head == tail)
        break;

      // Swap data[head] and data[tail]
      tmp = data[head];
      data[head] = data[tail];
      data[tail] = tmp;
    }

    return data;
  }

  /**
   * Reverses the byte order of <code>i</code>. If <code>i</code> is
   * Big Endian, the result will be Little Endian, and vice versa.
   *
   * @param i the value for which we want to reverse the byte order.
   * @return the value of <code>i</code> in reversed byte order.
   */
  public static short byteSwap(short i) {
    return (short) (((i & 0xFF) << 8) | ((i >> 8) & 0xFF));
  }

  /**
   * Reverses the byte order of <code>i</code>. If <code>i</code> is
   * Big Endian, the result will be Little Endian, and vice versa.
   *
   * @param i the value for which we want to reverse the byte order.
   * @return the value of <code>i</code> in reversed byte order.
   */
  public static char byteSwap(char i) {
    return (char) (((i & 0xFF) << 8) | ((i >> 8) & 0xFF));
  }

  /**
   * Reverses the byte order of <code>i</code>. If <code>i</code> is
   * Big Endian, the result will be Little Endian, and vice versa.
   *
   * @param i the value for which we want to reverse the byte order.
   * @return the value of <code>i</code> in reversed byte order.
   */
  public static int byteSwap(int i) {
    return (((i & 0xFF) << 24) |
      ((i & 0xFF00) << 8) |
      ((i >> 8) & 0xFF00) |
      ((i >> 24) & 0xFF));
  }

  /**
   * Reverses the byte order of <code>i</code>. If <code>i</code> is
   * Big Endian, the result will be Little Endian, and vice versa.
   *
   * @param i the value for which we want to reverse the byte order.
   * @return the value of <code>i</code> in reversed byte order.
   */
  public static long byteSwap(long i) {
    return (((i & 0xFFL) << 56) |
      ((i & 0xFF00L) << 40) |
      ((i & 0xFF0000L) << 24) |
      ((i & 0xFF000000L) << 8) |
      ((i >> 8) & 0xFF000000L) |
      ((i >> 24) & 0xFF0000L) |
      ((i >> 40) & 0xFF00L) |
      ((i >> 56) & 0xFFL));
  }

  /**
   * Writes the specified native type to <code>array</code> using Big Endian
   * representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, byte data) {
    array[pos + 0] = data;
  }

  /**
   * Writes the specified native type to <code>array</code> using Big Endian
   * representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, short data) {
    array[pos + 0] = (byte) ((data >> 8) & 0xFF);
    array[pos + 1] = (byte) ((data >> 0) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Big Endian
   * representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, char data) {
    array[pos + 0] = (byte) ((data >> 8) & 0xFF);
    array[pos + 1] = (byte) ((data >> 0) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Big Endian
   * representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, int data) {
    array[pos + 0] = (byte) ((data >> 24) & 0xFF);
    array[pos + 1] = (byte) ((data >> 16) & 0xFF);
    array[pos + 2] = (byte) ((data >> 8) & 0xFF);
    array[pos + 3] = (byte) ((data >> 0) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Big Endian
   * representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, long data) {
    array[pos + 0] = (byte) ((data >> 56) & 0xFF);
    array[pos + 1] = (byte) ((data >> 48) & 0xFF);
    array[pos + 2] = (byte) ((data >> 40) & 0xFF);
    array[pos + 3] = (byte) ((data >> 32) & 0xFF);
    array[pos + 4] = (byte) ((data >> 24) & 0xFF);
    array[pos + 5] = (byte) ((data >> 16) & 0xFF);
    array[pos + 6] = (byte) ((data >> 8) & 0xFF);
    array[pos + 7] = (byte) ((data >> 0) & 0xFF);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, char[] data) {
    arrayPutBE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutBE(byte[] array, int pos, char[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutBE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, short[] data) {
    arrayPutBE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutBE(byte[] array, int pos, short[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutBE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, int[] data) {
    arrayPutBE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutBE(byte[] array, int pos, int[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutBE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutBE(byte[] array, int pos, long[] data) {
    arrayPutBE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Big Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutBE(byte[] array, int pos, long[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutBE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified native type to <code>array</code> using Little
   * Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, byte data) {
    array[pos + 0] = data;
  }

  /**
   * Writes the specified native type to <code>array</code> using Little
   * Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, short data) {
    array[pos + 0] = (byte) ((data >> 0) & 0xFF);
    array[pos + 1] = (byte) ((data >> 8) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Little
   * Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, char data) {
    array[pos + 0] = (byte) ((data >> 0) & 0xFF);
    array[pos + 1] = (byte) ((data >> 8) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Little
   * Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, int data) {
    array[pos + 0] = (byte) ((data >> 0) & 0xFF);
    array[pos + 1] = (byte) ((data >> 8) & 0xFF);
    array[pos + 2] = (byte) ((data >> 16) & 0xFF);
    array[pos + 3] = (byte) ((data >> 24) & 0xFF);
  }

  /**
   * Writes the specified native type to <code>array</code> using Little
   * Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, long data) {
    array[pos + 0] = (byte) ((data >> 0) & 0xFF);
    array[pos + 1] = (byte) ((data >> 8) & 0xFF);
    array[pos + 2] = (byte) ((data >> 16) & 0xFF);
    array[pos + 3] = (byte) ((data >> 24) & 0xFF);
    array[pos + 4] = (byte) ((data >> 32) & 0xFF);
    array[pos + 5] = (byte) ((data >> 40) & 0xFF);
    array[pos + 6] = (byte) ((data >> 48) & 0xFF);
    array[pos + 7] = (byte) ((data >> 56) & 0xFF);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, char[] data) {
    arrayPutLE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutLE(byte[] array, int pos, char[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutLE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, short[] data) {
    arrayPutLE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutLE(byte[] array, int pos, short[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutLE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, int[] data) {
    arrayPutLE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutLE(byte[] array, int pos, int[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutLE(array, pos + i, data[offset + i]);
    }
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   */
  public static void arrayPutLE(byte[] array, int pos, long[] data) {
    arrayPutLE(array, pos, data, 0, data.length);
  }

  /**
   * Writes the specified array of native types to <code>array</code> using
   * Little Endian representation.
   *
   * @param array the array to which we should write.
   * @param pos the position in the array where writing should begin.
   * @param data the data to write.
   * @param offset offset into <code>data</code> where we should start
   *        reading.
   * @param length the number of array elements to put into <code>array<code>.
   */
  public static void arrayPutLE(byte[] array, int pos, long[] data,
    int offset, int length) {
    for (int i = 0; i < length; ++i) {
      arrayPutLE(array, pos + i, data[offset + i]);
    }
  }

  public static boolean booleanEnabledByProperties(boolean defaultValue,
    final String... debugProperties) {
    boolean debug = defaultValue;
    for (String debugProperty : debugProperties) {
      String value = System.getProperty(debugProperty);

      if (value == null)
        ;
      else if (value.equals("true")) {
        debug = true;
      } else if (value.equals("false")) {
        debug = false;
      } else {
        System.err.println("[WARNING] Unrecognized value for debug " +
          "property \"" + debugProperty + "\": \"" + value +
          "\"");
      }
    }

    return debug;
  }
}
