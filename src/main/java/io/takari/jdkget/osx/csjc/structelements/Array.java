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

package io.takari.jdkget.osx.csjc.structelements;

import io.takari.jdkget.osx.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class Array extends StructElement {

  private final StructElement[] elements;

  public Array(String typeName, StructElement[] elements) {
    super(typeName + "[" + elements.length + "]");
    this.elements = new StructElement[elements.length];
    for (int i = 0; i < this.elements.length; ++i) {
      this.elements[i] = elements[i];
    }
  }

  public StructElement[] getElements() {
    return Util.arrayCopy(elements, new StructElement[elements.length]);
  }
}
