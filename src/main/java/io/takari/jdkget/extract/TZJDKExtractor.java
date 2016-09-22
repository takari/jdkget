package io.takari.jdkget.extract;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.z.ZCompressorInputStream;

public class TZJDKExtractor extends AbstractTarJDKExtractor {

  @Override
  protected InputStream wrap(InputStream in) throws IOException {
    return new ZCompressorInputStream(in);
  }
  
}
