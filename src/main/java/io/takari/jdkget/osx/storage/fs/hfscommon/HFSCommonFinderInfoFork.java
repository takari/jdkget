/*-
 * Copyright (C) 2009 Erik Larsson
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

package io.takari.jdkget.osx.storage.fs.hfscommon;

import java.io.InputStream;
import java.io.OutputStream;

import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSFinderInfo;
import io.takari.jdkget.osx.io.RandomAccessStream;
import io.takari.jdkget.osx.io.ReadableByteArrayStream;
import io.takari.jdkget.osx.io.ReadableRandomAccessInputStream;
import io.takari.jdkget.osx.io.ReadableRandomAccessStream;
import io.takari.jdkget.osx.io.SynchronizedReadableRandomAccessStream;
import io.takari.jdkget.osx.io.TruncatableRandomAccessStream;
import io.takari.jdkget.osx.io.WritableRandomAccessStream;
import io.takari.jdkget.osx.storage.fs.FSFork;
import io.takari.jdkget.osx.storage.fs.FSForkType;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class HFSCommonFinderInfoFork implements FSFork {
  private final CommonHFSFinderInfo finderInfo;

  public HFSCommonFinderInfoFork(CommonHFSFinderInfo finderInfo) {
    this.finderInfo = finderInfo;
  }

  @Override
  public FSForkType getType() {
    return FSForkType.MACOS_FINDERINFO;
  }

  @Override
  public long getLength() {
    return 32;
  }

  @Override
  public long getOccupiedSize() {
    return 32;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isTruncatable() {
    return false;
  }

  @Override
  public boolean isCompressed() {
    return false;
  }

  @Override
  public String getForkIdentifier() {
    return "FinderInfo";
  }

  @Override
  public boolean hasXattrName() {
    return true;
  }

  @Override
  public String getXattrName() {
    return "com.apple.FinderInfo";
  }

  @Override
  public InputStream getInputStream() {
    return new ReadableRandomAccessInputStream(
      new SynchronizedReadableRandomAccessStream(
        getReadableRandomAccessStream()));
  }

  @Override
  public ReadableRandomAccessStream getReadableRandomAccessStream() {
    return new ReadableByteArrayStream(finderInfo.getBytes());
  }

  @Override
  public WritableRandomAccessStream getWritableRandomAccessStream() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public RandomAccessStream getRandomAccessStream() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public OutputStream getOutputStream() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public TruncatableRandomAccessStream getForkStream() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
