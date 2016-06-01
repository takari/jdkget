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
import java.util.Date;

import io.takari.jdkget.osx.csjc.PrintableStruct;
import io.takari.jdkget.osx.csjc.StructElements;
import io.takari.jdkget.osx.csjc.structelements.Dictionary;
import io.takari.jdkget.osx.hfs.types.hfs.CdrDirRec;
import io.takari.jdkget.osx.hfs.types.hfs.HFSDate;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusBSDInfo;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusCatalogFolder;
import io.takari.jdkget.osx.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public abstract class CommonHFSCatalogFolder implements CommonHFSCatalogAttributes, PrintableStruct, StructElements {
  public abstract CommonHFSCatalogNodeID getFolderID();

  public static CommonHFSCatalogFolder create(HFSPlusCatalogFolder data) {
    return new HFSPlusImplementation(data);
  }

  public static CommonHFSCatalogFolder create(CdrDirRec data) {
    return new HFSImplementation(data);
  }

  public abstract long getValence();

  public abstract int length();

  public abstract byte[] getBytes();

  @Override
  public CommonHFSCatalogNodeID getCatalogNodeID() {
    return getFolderID();
  }

  /* @Override */
  @Override
  public void print(PrintStream ps, String prefix) {
    ps.println(prefix + CommonHFSCatalogFolder.class.getSimpleName() + ":");
    printFields(ps, prefix + " ");
  }

  public static class HFSPlusImplementation extends CommonHFSCatalogFolder {
    private HFSPlusCatalogFolder data;

    public HFSPlusImplementation(HFSPlusCatalogFolder data) {
      this.data = data;
    }

    //@Deprecated
    public HFSPlusCatalogFolder getUnderlying() {
      return data;
    }

    @Override
    public CommonHFSCatalogNodeID getFolderID() {
      return CommonHFSCatalogNodeID.create(data.getFolderID());
    }

    @Override
    public long getValence() {
      return Util.unsign(data.getValence());
    }

    @Override
    public int length() {
      return HFSPlusCatalogFolder.length();
    }

    @Override
    public byte[] getBytes() {
      return data.getBytes();
    }

    /* @Override */
    @Override
    public short getRecordType() {
      return data.getRecordType();
    }

    /* @Override */
    @Override
    public short getFlags() {
      return data.getFlags();
    }

    /* @Override */
    @Override
    public int getCreateDate() {
      return data.getCreateDate();
    }

    /* @Override */
    @Override
    public int getContentModDate() {
      return data.getContentModDate();
    }

    /* @Override */
    @Override
    public int getAttributeModDate() {
      return data.getAttributeModDate();
    }

    /* @Override */
    @Override
    public int getAccessDate() {
      return data.getAccessDate();
    }

    /* @Override */
    @Override
    public int getBackupDate() {
      return data.getBackupDate();
    }

    /* @Override */
    @Override
    public Date getCreateDateAsDate() {
      return data.getCreateDateAsDate();
    }

    /* @Override */
    @Override
    public Date getContentModDateAsDate() {
      return data.getContentModDateAsDate();
    }

    /* @Override */
    @Override
    public Date getAttributeModDateAsDate() {
      return data.getAttributeModDateAsDate();
    }

    /* @Override */
    @Override
    public Date getAccessDateAsDate() {
      return data.getAccessDateAsDate();
    }

    /* @Override */
    @Override
    public Date getBackupDateAsDate() {
      return data.getBackupDateAsDate();
    }

    /* @Override */
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

    /* @Override */
    @Override
    public boolean hasPermissions() {
      return true;
    }

    /* @Override */
    @Override
    public HFSPlusBSDInfo getPermissions() {
      return data.getPermissions();
    }

    /* @Override */
    @Override
    public boolean hasCreateDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasContentModDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasAttributeModDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasAccessDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasBackupDate() {
      return true;
    }

    @Override
    public CommonHFSFinderInfo getFinderInfo() {
      return CommonHFSFinderInfo.create(data);
    }
  }

  public static class HFSImplementation extends CommonHFSCatalogFolder {
    private CdrDirRec data;

    public HFSImplementation(CdrDirRec data) {
      this.data = data;
    }

    @Override
    public CommonHFSCatalogNodeID getFolderID() {
      return CommonHFSCatalogNodeID.create(data.getDirDirID());
    }

    @Override
    public long getValence() {
      return Util.unsign(data.getDirVal());
    }

    @Override
    public int length() {
      return CdrDirRec.length();
    }

    @Override
    public byte[] getBytes() {
      return data.getBytes();
    }


    /* @Override */
    @Override
    public short getRecordType() {
      return data.getCdrType();
    }

    /* @Override */
    @Override
    public short getFlags() {
      return data.getDirFlags();
    }

    /* @Override */
    @Override
    public int getCreateDate() {
      return data.getDirCrDat();
    }

    /* @Override */
    @Override
    public int getContentModDate() {
      return data.getDirMdDat();
    }

    /* @Override */
    @Override
    public int getAttributeModDate() {
      return data.getDirMdDat();
    }

    /* @Override */
    @Override
    public int getAccessDate() {
      return data.getDirMdDat();
    }

    /* @Override */
    @Override
    public int getBackupDate() {
      return data.getDirBkDat();
    }

    /* @Override */
    @Override
    public Date getCreateDateAsDate() {
      return HFSDate.localTimestampToDate(getCreateDate());
    }

    /* @Override */
    @Override
    public Date getContentModDateAsDate() {
      return HFSDate.localTimestampToDate(getContentModDate());
    }

    /* @Override */
    @Override
    public Date getAttributeModDateAsDate() {
      return HFSDate.localTimestampToDate(getAttributeModDate());
    }

    /* @Override */
    @Override
    public Date getAccessDateAsDate() {
      return HFSDate.localTimestampToDate(getAccessDate());
    }

    /* @Override */
    @Override
    public Date getBackupDateAsDate() {
      return HFSDate.localTimestampToDate(getBackupDate());
    }

    /* @Override */
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

    /* @Override */
    @Override
    public boolean hasPermissions() {
      return false;
    }

    /* @Override */
    @Override
    public HFSPlusBSDInfo getPermissions() {
      throw new UnsupportedOperationException("Not supported.");
    }

    /* @Override */
    @Override
    public boolean hasAccessDate() {
      return false;
    }

    /* @Override */
    @Override
    public boolean hasBackupDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasCreateDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasContentModDate() {
      return true;
    }

    /* @Override */
    @Override
    public boolean hasAttributeModDate() {
      return false;
    }

    @Override
    public CommonHFSFinderInfo getFinderInfo() {
      return CommonHFSFinderInfo.create(data);
    }
  }
}
