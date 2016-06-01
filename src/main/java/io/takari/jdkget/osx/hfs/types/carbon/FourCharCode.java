/*-
 * Copyright (C) 2006 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.jdkget.osx.hfs.types.carbon;

import java.io.PrintStream;

import io.takari.jdkget.osx.csjc.PrintableStruct;
import io.takari.jdkget.osx.csjc.StructElements;
import io.takari.jdkget.osx.csjc.structelements.Dictionary;
import io.takari.jdkget.osx.util.Util;

public class FourCharCode implements PrintableStruct, StructElements {
  /*
   * struct FourCharCode
   * size: 4 bytes
   * description: a typedef originally
   *
   * BP  Size  Type    Identifier    Description
   * -------------------------------------------
   * 0   4     UInt32  fourCharCode
   */

  private final byte[] fourCharCode = new byte[4];

  public FourCharCode(byte[] data, int offset) {
    System.arraycopy(data, offset + 0, fourCharCode, 0, 4);
  }

  public int getFourCharCode() {
    return Util.readIntBE(fourCharCode);
  }

  public String getFourCharCodeAsString() {
    return Util.toASCIIString(getFourCharCode());
  }

  @Override
  public void printFields(PrintStream ps, String prefix) {
    ps.println(prefix + " fourCharCode: \"" + getFourCharCodeAsString() + "\"");
  }

  @Override
  public void print(PrintStream ps, String prefix) {
    ps.println(prefix + "FourCharCode:");
    printFields(ps, prefix);
  }

  public byte[] getBytes() {
    return Util.createCopy(fourCharCode);
  }

  /* @Override */
  @Override
  public Dictionary getStructElements() {
    DictionaryBuilder db = new DictionaryBuilder(FourCharCode.class.getSimpleName());

    db.addEncodedString("fourCharCode", fourCharCode, "US-ASCII");

    return db.getResult();
  }
}
