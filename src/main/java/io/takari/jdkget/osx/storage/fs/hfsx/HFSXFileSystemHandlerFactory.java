/*-
 * Copyright (C) 2008-2014 Erik Larsson
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

package io.takari.jdkget.osx.storage.fs.hfsx;

import io.takari.jdkget.osx.storage.fs.DefaultFileSystemHandlerInfo;
import io.takari.jdkget.osx.storage.fs.FileSystemCapability;
import io.takari.jdkget.osx.storage.fs.FileSystemHandler;
import io.takari.jdkget.osx.storage.fs.FileSystemHandlerFactory;
import io.takari.jdkget.osx.storage.fs.FileSystemHandlerInfo;
import io.takari.jdkget.osx.storage.fs.FileSystemRecognizer;
import io.takari.jdkget.osx.storage.fs.hfscommon.HFSCommonFileSystemHandler;
import io.takari.jdkget.osx.storage.fs.hfsplus.HFSPlusFileSystemHandlerFactory;
import io.takari.jdkget.osx.storage.io.DataLocator;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class HFSXFileSystemHandlerFactory extends HFSPlusFileSystemHandlerFactory {
  private static final FileSystemRecognizer recognizer = new HFSXFileSystemRecognizer();

  private static final FileSystemHandlerInfo handlerInfo =
    new DefaultFileSystemHandlerInfo("org.catacombae.hfsx_handler",
      "HFSX file system handler", "1.0", 0, "Erik Larsson, Catacombae Software");

  @Override
  public FileSystemCapability[] getCapabilities() {
    return HFSCommonFileSystemHandler.getStaticCapabilities();
  }

  @Override
  protected FileSystemHandler createHandlerInternal(DataLocator data,
    boolean useCaching, boolean posixFilenames, boolean composeFilename,
    boolean hideProtected) {
    return new HFSXFileSystemHandler(data, useCaching, posixFilenames,
      composeFilename, hideProtected);
  }

  @Override
  public FileSystemHandlerInfo getHandlerInfo() {
    return handlerInfo;
  }

  @Override
  public FileSystemHandlerFactory newInstance() {
    return new HFSXFileSystemHandlerFactory();
  }

  @Override
  public FileSystemRecognizer getRecognizer() {
    return recognizer;
  }
}
