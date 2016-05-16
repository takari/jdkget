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

package io.takari.osxjdkget.storage.ps.apm;

import io.takari.osxjdkget.storage.io.DataLocator;
import io.takari.osxjdkget.storage.ps.PartitionSystemHandler;
import io.takari.osxjdkget.storage.ps.PartitionSystemHandlerFactory;
import io.takari.osxjdkget.storage.ps.PartitionSystemImplementationInfo;
import io.takari.osxjdkget.storage.ps.PartitionSystemRecognizer;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class APMHandlerFactory extends PartitionSystemHandlerFactory {
  private static final APMRecognizer recognizer = new APMRecognizer();

  @Override
  public PartitionSystemHandler createHandler(DataLocator partitionData) {
    return new APMHandler(partitionData);
  }

  @Override
  public PartitionSystemImplementationInfo getInfo() {
    return new PartitionSystemImplementationInfo("Apple Partition Map",
      "Catacombae APM PS Handler", "1.0", "Catacombae");
  }

  @Override
  public PartitionSystemRecognizer getRecognizer() {
    return recognizer;
  }

}
