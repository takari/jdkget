/*-
 * Copyright (C) 2006-2009 Erik Larsson
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

package io.takari.jdkget.osx.hfs.x;

import io.takari.jdkget.osx.hfs.plus.HFSPlusVolume;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonBTHeaderRecord;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogIndexNode;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogKey;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogLeafNode;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogLeafRecord;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogNodeID;
import io.takari.jdkget.osx.hfs.types.hfscommon.CommonHFSCatalogString;
import io.takari.jdkget.osx.hfs.types.hfsplus.BTHeaderRec;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSCatalogNodeID;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusVolumeHeader;
import io.takari.jdkget.osx.hfs.types.hfsplus.HFSUniStr255;
import io.takari.jdkget.osx.hfs.types.hfsx.HFSXCatalogKey;
import io.takari.jdkget.osx.io.ReadableRandomAccessStream;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class HFSXVolume extends HFSPlusVolume {
  private final byte keyCompareType;

  public HFSXVolume(ReadableRandomAccessStream hfsFile,
    boolean cachingEnabled) {

    super(hfsFile, cachingEnabled, HFSPlusVolumeHeader.SIGNATURE_HFSX);

    CommonBTHeaderRecord.CompareType keyCompareTypeEnum =
      getCatalogFile().getCatalogHeaderNode().getHeaderRecord().getKeyCompareType();


    switch (keyCompareTypeEnum) {
      case CASE_FOLDING:
        this.keyCompareType = BTHeaderRec.kHFSCaseFolding;
        break;
      case BINARY_COMPARE:
        this.keyCompareType = BTHeaderRec.kHFSBinaryCompare;
        break;
      default:
        throw new RuntimeException("Unknown key compare type:" +
          keyCompareTypeEnum);
    }
  }

  @Override
  public CommonHFSCatalogIndexNode newCatalogIndexNode(byte[] data,
    int offset, int nodeSize) {
    return CommonHFSCatalogIndexNode.createHFSX(data, offset, nodeSize,
      keyCompareType);
  }

  @Override
  public CommonHFSCatalogKey newCatalogKey(CommonHFSCatalogNodeID nodeID,
    CommonHFSCatalogString searchString) {
    return CommonHFSCatalogKey.create(new HFSXCatalogKey(
      new HFSCatalogNodeID((int) nodeID.toLong()),
      new HFSUniStr255(searchString.getStructBytes(), 0),
      keyCompareType));
  }

  @Override
  public CommonHFSCatalogLeafNode newCatalogLeafNode(byte[] data, int offset,
    int nodeSize) {
    return CommonHFSCatalogLeafNode.createHFSX(data, offset, nodeSize,
      keyCompareType);
  }

  @Override
  public CommonHFSCatalogLeafRecord newCatalogLeafRecord(byte[] data,
    int offset) {
    return CommonHFSCatalogLeafRecord.createHFSX(data, offset,
      offset + data.length, keyCompareType);
  }
}
