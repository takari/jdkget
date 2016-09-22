package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import io.takari.jdkget.extract.AbstractTarJDKExtractor;

public class TgzJDKExtractor extends AbstractTarJDKExtractor {

  @Override
  protected InputStream wrap(InputStream in) throws IOException {
    return new GZIPInputStream(in);
  }
  
}
