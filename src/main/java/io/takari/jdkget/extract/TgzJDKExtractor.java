package io.takari.jdkget.extract;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class TgzJDKExtractor extends AbstractTarJDKExtractor {

  @Override
  protected InputStream wrap(InputStream in) throws IOException {
    return new GZIPInputStream(in);
  }
  
}
