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

package io.takari.osxjdkget.storage.ps.gpt;

import io.takari.osxjdkget.io.ReadableRandomAccessStream;
import io.takari.osxjdkget.storage.io.DataLocator;
import io.takari.osxjdkget.storage.ps.Partition;
import io.takari.osxjdkget.storage.ps.PartitionSystemHandler;
import io.takari.osxjdkget.storage.ps.gpt.types.GUIDPartitionTable;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class GPTHandler extends PartitionSystemHandler {

  private DataLocator partitionData;

  public GPTHandler(DataLocator partitionData) {
    this.partitionData = partitionData;
  }

  @Override
  public long getPartitionCount() {
    GUIDPartitionTable gpt = readPartitionTable();
    return gpt.getUsedPartitionCount();
  }

  @Override
  public Partition[] getPartitions() {
    GUIDPartitionTable partitionTable = readPartitionTable();
    return partitionTable.getUsedPartitionEntries();
  }

  @Override
  public void close() {
    partitionData.close();
  }

  public GUIDPartitionTable readPartitionTable() {
    ReadableRandomAccessStream llf = null;
    try {
      llf = partitionData.createReadOnlyFile();
      GUIDPartitionTable gpt = new GUIDPartitionTable(llf, 0);

      if (gpt.isValid())
        return gpt;
      else
        return null;
    } finally {
      if (llf != null)
        llf.close();
    }
  }

}
