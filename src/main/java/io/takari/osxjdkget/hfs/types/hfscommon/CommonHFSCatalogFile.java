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

package io.takari.osxjdkget.hfs.types.hfscommon;

import java.io.PrintStream;
import java.util.Date;

import io.takari.osxjdkget.csjc.PrintableStruct;
import io.takari.osxjdkget.csjc.StaticStruct;
import io.takari.osxjdkget.csjc.StructElements;
import io.takari.osxjdkget.csjc.structelements.Dictionary;
import io.takari.osxjdkget.hfs.types.hfs.CdrFilRec;
import io.takari.osxjdkget.hfs.types.hfs.HFSDate;
import io.takari.osxjdkget.hfs.types.hfsplus.HFSPlusBSDInfo;
import io.takari.osxjdkget.hfs.types.hfsplus.HFSPlusCatalogFile;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public abstract class CommonHFSCatalogFile
  implements StaticStruct, PrintableStruct, StructElements,
  CommonHFSCatalogAttributes {
  public abstract CommonHFSCatalogNodeID getFileID();

  public abstract CommonHFSForkData getDataFork();

  public abstract CommonHFSForkData getResourceFork();

  @Override
  public abstract byte[] getBytes();

  public abstract boolean isHardFileLink();

  public abstract boolean isHardDirectoryLink();

  public abstract boolean isSymbolicLink();

  public abstract int getHardLinkInode();

  @Override
  public CommonHFSCatalogNodeID getCatalogNodeID() {
    return getFileID();
  }

  @Override
  public void print(PrintStream ps, String prefix) {
    ps.println(prefix + CommonHFSCatalogFile.class.getSimpleName() + ":");
    printFields(ps, prefix + " ");
  }

  @Override
  public abstract void printFields(PrintStream ps, String string);

  public static CommonHFSCatalogFile create(HFSPlusCatalogFile data) {
    return new HFSPlusImplementation(data);
  }

  public static CommonHFSCatalogFile create(CdrFilRec data) {
    return new HFSImplementation(data);
  }

  public static class HFSPlusImplementation extends CommonHFSCatalogFile {
    private static final int HARD_FILE_LINK_FILE_TYPE = 0x686C6E6B; // "hlnk"
    private static final int HARD_FILE_LINK_CREATOR = 0x6866732B; // "hfs+"
    private static final int HARD_DIRECTORY_LINK_FILE_TYPE = 0x66647270; // "fdrp"
    private static final int HARD_DIRECTORY_LINK_CREATOR = 0x4d414353; // "MACS"
    private HFSPlusCatalogFile data;

    private HFSPlusImplementation(HFSPlusCatalogFile data) {
      this.data = data;
    }

    //@Deprecated
    public HFSPlusCatalogFile getUnderlying() {
      return data;
    }

    @Override
    public CommonHFSCatalogNodeID getFileID() {
      return CommonHFSCatalogNodeID.create(data.getFileID());
    }

    @Override
    public CommonHFSForkData getDataFork() {
      return CommonHFSForkData.create(data.getDataFork());
    }

    @Override
    public CommonHFSForkData getResourceFork() {
      return CommonHFSForkData.create(data.getResourceFork());
    }

    /* @Override */
    @Override
    public int size() {
      return data.size();
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

    @Override
    public void printFields(PrintStream ps, String prefix) {
      ps.println(prefix + "data:");
      data.print(ps, prefix + " ");
    }

    @Override
    public boolean isSymbolicLink() {
      return data.getPermissions().getFileModeFileType() == HFSPlusBSDInfo.FILETYPE_SYMBOLIC_LINK;
    }

    /* @Override */
    @Override
    public Dictionary getStructElements() {
      return data.getStructElements();
    }

    @Override
    public boolean hasPermissions() {
      return true;
    }

    @Override
    public HFSPlusBSDInfo getPermissions() {
      return data.getPermissions();
    }

    @Override
    public boolean isHardFileLink() {
      int fileType = data.getUserInfo().getFileType().getOSType().getFourCharCode();
      int creator = data.getUserInfo().getFileCreator().getOSType().getFourCharCode();
      return fileType == HARD_FILE_LINK_FILE_TYPE && creator == HARD_FILE_LINK_CREATOR;
    }

    @Override
    public boolean isHardDirectoryLink() {
      int fileType = data.getUserInfo().getFileType().getOSType().getFourCharCode();
      int creator = data.getUserInfo().getFileCreator().getOSType().getFourCharCode();
      return fileType == HARD_DIRECTORY_LINK_FILE_TYPE &&
        creator == HARD_DIRECTORY_LINK_CREATOR &&
        data.getHasLinkChainFlag();
    }

    @Override
    public int getHardLinkInode() {
      return data.getPermissions().getSpecial();
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

  public static class HFSImplementation extends CommonHFSCatalogFile {
    private CdrFilRec data;

    private HFSImplementation(CdrFilRec data) {
      this.data = data;
    }

    @Override
    public CommonHFSCatalogNodeID getFileID() {
      return CommonHFSCatalogNodeID.create(data.getFilFlNum());
    }

    @Override
    public CommonHFSForkData getDataFork() {
      return CommonHFSForkData.create(data.getFilExtRec(), data.getFilLgLen());
    }

    @Override
    public CommonHFSForkData getResourceFork() {
      return CommonHFSForkData.create(data.getFilRExtRec(), data.getFilRLgLen());
    }

    /* @Override */
    @Override
    public int size() {
      return data.size();
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
      return data.getFilFlags();
    }

    /* @Override */
    @Override
    public int getCreateDate() {
      return data.getFilCrDat();
    }

    /* @Override */
    @Override
    public int getContentModDate() {
      return data.getFilMdDat();
    }

    /* @Override */
    @Override
    public int getAttributeModDate() {
      return data.getFilMdDat();
    }

    /* @Override */
    @Override
    public int getAccessDate() {
      return data.getFilMdDat();
    }

    /* @Override */
    @Override
    public int getBackupDate() {
      return data.getFilBkDat();
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

    @Override
    public void printFields(PrintStream ps, String prefix) {
      ps.println(prefix + "data:");
      data.print(ps, prefix + " ");
    }

    @Override
    public boolean isSymbolicLink() {
      // HFS doesn't support symbolic links.
      return false;
    }

    /* @Override */
    @Override
    public Dictionary getStructElements() {
      return data.getStructElements();
    }

    @Override
    public boolean hasPermissions() {
      return false;
    }

    @Override
    public HFSPlusBSDInfo getPermissions() {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isHardFileLink() {
      return false; // No such thing in HFS.
    }

    @Override
    public boolean isHardDirectoryLink() {
      return false; // No such thing in HFS.
    }

    @Override
    public int getHardLinkInode() {
      throw new UnsupportedOperationException("Not supported for HFS.");
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

    @Override
    public CommonHFSFinderInfo getFinderInfo() {
      return CommonHFSFinderInfo.create(data);
    }
  }
}
