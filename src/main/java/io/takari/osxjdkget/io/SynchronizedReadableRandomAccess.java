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
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.takari.osxjdkget.io;

/**
 *
 * @author <a href="http://hem.bredband.net/catacombae">Erik Larsson</a>
 */
public interface SynchronizedReadableRandomAccess extends SynchronizedReadable, RandomAccess {
  /**
   * All open substreams of this stream must be added as references so that the synchronized
   * stream knows when it is safe to <code>close();</code> itself.
   * 
   * @param referrer the object referring to this stream.
   */
  public void addReference(Object referrer);

  /**
   * When a substream closes, it must remove itself from the reference list of the parent stream.
   * 
   * @param referrer the object referring to this stream.
   */
  public void removeReference(Object referrer);
}
