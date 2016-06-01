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
import io.takari.jdkget.osx.csjc.StructElements;
import io.takari.jdkget.osx.csjc.structelements.Dictionary;
import io.takari.jdkget.osx.hfs.types.hfs.CdrFThdRec;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusCatalogThread;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public abstract class CommonHFSCatalogFileThread extends CommonHFSCatalogThread
  implements PrintableStruct, StructElements {
  public abstract int length();

  public abstract byte[] getBytes();

  /* @Override */
  @Override
  public void print(PrintStream ps, String prefix) {
    ps.println(prefix + CommonHFSCatalogFileThread.class.getSimpleName() + ":");
    printFields(ps, prefix + " ");
  }

  public static CommonHFSCatalogFileThread create(HFSPlusCatalogThread data) {
    return new HFSPlusImplementation(data);
  }

  public static CommonHFSCatalogFileThread create(CdrFThdRec data) {
    return new HFSImplementation(data);
  }

  private static class HFSPlusImplementation extends CommonHFSCatalogFileThread {
    private final HFSPlusCatalogThread data;

    public HFSPlusImplementation(HFSPlusCatalogThread data) {
      this.data = data;
    }

    @Override
    public int length() {
      return data.length();
    }

    @Override
    public CommonHFSCatalogNodeID getParentID() {
      return CommonHFSCatalogNodeID.create(data.getParentID());
    }

    @Override
    public CommonHFSCatalogString getNodeName() {
      return CommonHFSCatalogString.createHFSPlus(data.getNodeName());
    }

    @Override
    public byte[] getBytes() {
      return data.getBytes();
    }

    @Override
    public void printFields(PrintStream ps, String prefix) {
      ps.println(prefix + "data:");
      data.print(ps, prefix + " ");
    }

    /* @Override */
    @Override
    public Dictionary getStructElements() {
      return data.getStructElements();
    }
  }

  private static class HFSImplementation extends CommonHFSCatalogFileThread {
    private final CdrFThdRec data;

    public HFSImplementation(CdrFThdRec data) {
      this.data = data;
    }

    @Override
    public int length() {
      return CdrFThdRec.length();
    }

    @Override
    public CommonHFSCatalogNodeID getParentID() {
      return CommonHFSCatalogNodeID.create(data.getFthdParID());
    }

    @Override
    public CommonHFSCatalogString getNodeName() {
      return CommonHFSCatalogString.createHFS(data.getFthdCName());
    }

    @Override
    public byte[] getBytes() {
      return data.getBytes();
    }

    @Override
    public void printFields(PrintStream ps, String prefix) {
      ps.println(prefix + "data:");
      data.print(ps, prefix + " ");
    }

    /* @Override */
    @Override
    public Dictionary getStructElements() {
      return data.getStructElements();
    }
  }
}
