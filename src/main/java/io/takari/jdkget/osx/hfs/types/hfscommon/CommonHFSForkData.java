/*-
 * Copyright (C) 2008 Erik Larsson
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

package io.takari.jdkget.osx.hfs.types.hfscommon;

import java.io.PrintStream;

import io.takari.jdkget.osx.csjc.PrintableStruct;
import io.takari.jdkget.osx.hfs.types.hfs.ExtDataRec;
import io.takari.jdkget.osx.hfs.types.hfs.ExtDescriptor;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusExtentDescriptor;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusForkData;
import io.takari.jdkget.osx.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public abstract class CommonHFSForkData implements PrintableStruct {
  public abstract boolean hasTotalBlocks();

  public abstract long getTotalBlocks();

  public abstract long getLogicalSize();

  public abstract CommonHFSExtentDescriptor[] getBasicExtents();

  public static CommonHFSForkData create(ExtDataRec edr, long logicalSize) {
    return new HFSImplementation(edr, logicalSize);
  }

  public static CommonHFSForkData create(HFSPlusForkData hper) {
    return new HFSPlusImplementation(hper);
  }

  public static class HFSImplementation extends CommonHFSForkData {
    private final ExtDataRec edr;
    private final long logicalSize;

    public HFSImplementation(ExtDataRec edr, long logicalSize) {
      this.edr = edr;
      this.logicalSize = logicalSize;
    }

    @Override
    public final boolean hasTotalBlocks() {
      return false;
    }

    @Override
    public final long getTotalBlocks() {
      throw new UnsupportedOperationException("Information about the " +
        "total number of blocks in a fork does not exist in HFS.");
    }

    @Override
    public long getLogicalSize() {
      return logicalSize;
    }

    @Override
    public CommonHFSExtentDescriptor[] getBasicExtents() {
      ExtDescriptor[] src = edr.getExtDataRecs();
      CommonHFSExtentDescriptor[] result = new CommonHFSExtentDescriptor[src.length];
      for (int i = 0; i < result.length; ++i) {
        result[i] = CommonHFSExtentDescriptor.create(src[i]);
      }
      return result;
    }

    /* @Override */
    @Override
    public void print(PrintStream ps, String prefix) {
      edr.print(ps, prefix);
    }

    /* @Override */
    @Override
    public void printFields(PrintStream ps, String prefix) {
      edr.printFields(ps, prefix);
    }
  }

  public static class HFSPlusImplementation extends CommonHFSForkData {
    private final HFSPlusForkData hper;

    public HFSPlusImplementation(HFSPlusForkData hper) {
      this.hper = hper;
    }

    @Override
    public final boolean hasTotalBlocks() {
      return true;
    }

    @Override
    public final long getTotalBlocks() {
      return Util.unsign(hper.getTotalBlocks());
    }

    @Override
    public long getLogicalSize() {
      return hper.getLogicalSize();
    }

    @Override
    public CommonHFSExtentDescriptor[] getBasicExtents() {
      HFSPlusExtentDescriptor[] src = hper.getExtents().getExtentDescriptors();
      CommonHFSExtentDescriptor[] result = new CommonHFSExtentDescriptor[src.length];
      for (int i = 0; i < result.length; ++i) {
        result[i] = CommonHFSExtentDescriptor.create(src[i]);
      }
      return result;
    }

    /* @Override */
    @Override
    public void print(PrintStream ps, String prefix) {
      hper.print(ps, prefix);
    }

    /* @Override */
    @Override
    public void printFields(PrintStream ps, String prefix) {
      hper.printFields(ps, prefix);
    }
  }
}
