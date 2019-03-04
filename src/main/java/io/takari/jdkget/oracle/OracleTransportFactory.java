package io.takari.jdkget.oracle;

import java.util.Map;

import io.takari.jdkget.ITransport;
import io.takari.jdkget.ITransportFactory;

public class OracleTransportFactory implements ITransportFactory {
  private static final long serialVersionUID = 1L;

  @Override
  public ITransport createTransport(Map<String, String> parameters) {
    if (parameters != null) {
      String website = parameters.get(PARAM_BASEURL);
      String otnUsername = parameters.get(PARAM_USERNAME);
      String otnPassword = parameters.get(PARAM_PASSWORD);

      if (website != null || otnUsername != null || otnPassword != null) {
        return new OracleWebsiteTransport(website, otnUsername, otnPassword);
      }
    }
    return new OracleWebsiteTransport();
  }

}
