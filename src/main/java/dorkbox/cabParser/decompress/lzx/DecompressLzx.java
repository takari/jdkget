/*
 * Copyright 2012 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package dorkbox.cabParser.decompress.lzx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

import dorkbox.cabParser.CabException;
import dorkbox.cabParser.decompress.Decompressor;
import io.takari.jdkget.extract.LzxDecompressor;

/**
 * Replacement for CabParser's DecompressLzx that works with latest (9.0.1+) versions of jdk for windows
 */
public final class DecompressLzx implements Decompressor {

  private LzxDecompressor lzx;

  public DecompressLzx() {}

  @Override
  public void init(int windowBits) throws CabException {
    reset(windowBits);
  }

  @Override
  public void reset(int windowBits) throws CabException {
    lzx = new LzxDecompressor();
    lzx.init(windowBits, 0, 4096, 0, false);
  }

  @Override
  public void decompress(byte[] inputBytes, byte[] outputBytes, int inputLength, int outputLength) throws CabException {

    InputStream in = new ByteArrayInputStream(inputBytes, 0, inputLength);
    ByteArrayOutputStream out = new ByteArrayOutputStream(outputLength);
    try {
      lzx.setOutputLength(lzx.getOutputLength() + outputLength);
      lzx.decompress(in, out, outputLength);
      System.arraycopy(out.toByteArray(), 0, outputBytes, 0, outputLength);
    } catch (IOException e) {
      CabException ce = new CabException(e.getMessage());
      ce.initCause(e);
      throw ce;
    }

  }

  @Override
  public int getMaxGrowth() {
    return LZXConstants.MAX_GROWTH;
  }

}
