package io.takari.jdkget;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public interface ITransportFactory extends Serializable {

  public static final String PARAM_BASEURL = "io.takari.jdkget.baseUrl";
  public static final String PARAM_USERNAME = "io.takari.jdkget.username";
  public static final String PARAM_PASSWORD = "io.takari.jdkget.password";

  ITransport createTransport(Map<String, String> parameters);

  default ITransport createTransport() {
    Map<String, String> trParams = new HashMap<>();
    Properties sprops = System.getProperties();
    sprops.forEach((k, val) -> trParams.put(k.toString(), val.toString()));
    return createTransport(trParams);
  }
}
