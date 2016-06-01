/*-
 * Copyright (C) 2009 Erik Larsson
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
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.jdkget.osx.csjc.structelements;

import java.lang.reflect.Field;

import io.takari.jdkget.osx.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class IntegerFieldDataHandle implements DataHandle {

  private final Field field;
  private final Object object;
  private final int length;

  public IntegerFieldDataHandle(Object object, Field field, int length) {

    switch (length) {
      case 1:
      case 2:
      case 4:
      case 8:
        break;
      default:
        throw new IllegalArgumentException("Invalid length: " + length);
    }

    this.object = object;
    this.field = field;
    this.length = length;
  }

  @Override
  public byte[] getBytesAsCopy() {
    try {
      byte[] res;

      switch (length) {
        case 1:
          res = Util.toByteArrayBE(field.getByte(object));
          break;
        case 2:
          res = Util.toByteArrayBE(field.getShort(object));
          break;
        case 4:
          res = Util.toByteArrayBE(field.getInt(object));
          break;
        case 8:
          res = Util.toByteArrayBE(field.getLong(object));
          break;
        default:
          throw new RuntimeException(); // Won't happen.
      }

      return res;
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Illegal access while trying to " +
        "read field: [" + field, e);
    }
  }

  @Override
  public byte[] getBytesAsCopy(int offset, int length) {
    return Util.createCopy(getBytesAsCopy(), offset, length);
  }

  @Override
  public int getLength() {
    return length;
  }
}
