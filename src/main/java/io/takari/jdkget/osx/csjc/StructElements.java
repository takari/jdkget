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
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.jdkget.osx.csjc;

import io.takari.jdkget.osx.csjc.structelements.Dictionary;
import io.takari.jdkget.osx.csjc.structelements.Endianness;
import io.takari.jdkget.osx.csjc.structelements.FieldType;
import io.takari.jdkget.osx.csjc.structelements.IntegerFieldBits;
import io.takari.jdkget.osx.csjc.structelements.IntegerFieldRepresentation;
import io.takari.jdkget.osx.csjc.structelements.Signedness;

/**
 * Interface to implement when a struct wants to present detailed information on
 * its members to an external program, also allowing its members to be modified
 * externally in a controlled way.
 *
 * @see io.takari.jdkget.osx.csjc.structelements.StructElement
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public interface StructElements {
  /** Shorthand constant. */
  public static final Endianness BIG_ENDIAN = Endianness.BIG_ENDIAN;
  /** Shorthand constant. */
  public static final Endianness LITTLE_ENDIAN = Endianness.LITTLE_ENDIAN;
  /** Shorthand constant. */
  public static final Signedness SIGNED = Signedness.SIGNED;
  /** Shorthand constant. */
  public static final Signedness UNSIGNED = Signedness.UNSIGNED;
  /** Shorthand constant. */
  public static final FieldType BOOLEAN = FieldType.BOOLEAN;
  /** Shorthand constant. */
  public static final FieldType INTEGER = FieldType.INTEGER;
  /** Shorthand constant. */
  public static final FieldType BYTEARRAY = FieldType.BYTEARRAY;
  /** Shorthand constant. */
  public static final FieldType ASCIISTRING = FieldType.ASCIISTRING;
  /** Shorthand constant. */
  public static final FieldType CUSTOM_CHARSET_STRING = FieldType.CUSTOM_CHARSET_STRING;
  /** Shorthand constant. */
  public static final FieldType DATE = FieldType.DATE;
  /** Shorthand constant. */
  public static final IntegerFieldBits BITS_8 = IntegerFieldBits.BITS_8;
  /** Shorthand constant. */
  public static final IntegerFieldBits BITS_16 = IntegerFieldBits.BITS_16;
  /** Shorthand constant. */
  public static final IntegerFieldBits BITS_32 = IntegerFieldBits.BITS_32;
  /** Shorthand constant. */
  public static final IntegerFieldBits BITS_64 = IntegerFieldBits.BITS_64;
  /** Shorthand constant. */
  public static final IntegerFieldRepresentation DECIMAL = IntegerFieldRepresentation.DECIMAL;
  /** Shorthand constant. */
  public static final IntegerFieldRepresentation HEXADECIMAL = IntegerFieldRepresentation.HEXADECIMAL;
  /** Shorthand constant. */
  public static final IntegerFieldRepresentation OCTAL = IntegerFieldRepresentation.OCTAL;
  /** Shorthand constant. */
  public static final IntegerFieldRepresentation BINARY = IntegerFieldRepresentation.BINARY;

  /**
   * Shorthand subclass, so the user doesn't have to import DictionaryBuilder in every
   * implementation of StructElements.
   */
  public class DictionaryBuilder extends io.takari.jdkget.osx.csjc.structelements.DictionaryBuilder {
    /**
     * @see io.takari.jdkget.osx.csjc.structelements.DictionaryBuilder#DictionaryBuilder(java.lang.String)
     */
    public DictionaryBuilder(String typeName) {
      super(typeName);
    }

    /**
     * @see io.takari.jdkget.osx.csjc.structelements.DictionaryBuilder#DictionaryBuilder(java.lang.String, java.lang.String)
     */
    public DictionaryBuilder(String typeName, String typeDescription) {
      super(typeName, typeDescription);
    }
  }

  /**
   * Returns a dictionary of the elements of this data structure. The keys in
   * the dictionary should be the respective variable names, and the elements
   * should provide access to all the fields of the data structure.
   *
   * @return a dictionary of the elements of this data structure.
   */
  public Dictionary getStructElements();
}
