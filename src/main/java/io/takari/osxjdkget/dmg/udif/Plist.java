/*-
 * Copyright (C) 2006-2008 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.osxjdkget.dmg.udif;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.LinkedList;

import io.takari.osxjdkget.io.ReaderInputStream;
import io.takari.osxjdkget.plist.PlistNode;
import io.takari.osxjdkget.plist.XmlPlist;
import net.iharder.Base64;

public class Plist extends XmlPlist {

  public Plist(byte[] data) {
    this(data, 0, data.length);
  }

  public Plist(byte[] data, boolean useSAXParser) {
    this(data, 0, data.length, useSAXParser);
  }

  public Plist(byte[] data, int offset, int length) {
    this(data, offset, length, false);
  }

  public Plist(byte[] data, int offset, int length, boolean useSAXParser) {
    super(data, offset, length, useSAXParser);
  }

  //public byte[] getData() { return Util.createCopy(plistData); }

  public PlistPartition[] getPartitions() throws IOException {
    LinkedList<PlistPartition> partitionList = new LinkedList<PlistPartition>();
    PlistNode current = getRootNode();
    current = current.cd("dict");
    current = current.cdkey("resource-fork");
    current = current.cdkey("blkx");

    // Variables to keep track of the pointers of the previous partition
    long previousOutOffset = 0;
    long previousInOffset = 0;

    // Iterate over the partitions and gather data
    for (PlistNode pn : current.getChildren()) {
      String partitionName = io.takari.osxjdkget.util.Util.readFully(pn.getKeyValue("Name"));
      String partitionID = io.takari.osxjdkget.util.Util.readFully(pn.getKeyValue("ID"));
      String partitionAttributes = io.takari.osxjdkget.util.Util.readFully(pn.getKeyValue("Attributes"));
      Reader base64Data = pn.getKeyValue("Data");
      InputStream base64DataInputStream = new Base64.InputStream(new ReaderInputStream(base64Data, Charset.forName("US-ASCII")));
      PlistPartition dpp = new PlistPartition(partitionName, partitionID, partitionAttributes, base64DataInputStream, previousOutOffset, previousInOffset);
      previousOutOffset = dpp.getFinalOutOffset();
      previousInOffset = dpp.getFinalInOffset();
      partitionList.addLast(dpp);
    }

    return partitionList.toArray(new PlistPartition[partitionList.size()]);
  }
}
