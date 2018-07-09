package io.takari.jdkget;

import java.io.File;
import java.io.IOException;

public interface ITransport {
  
  interface Configurable extends ITransport {
    static int CONNECTION_REQUEST_TIMEOUT = 1 * 1000;
    static int CONNECT_TIMEOUT = 30 * 1000;
    static int SOCKET_TIMEOUT = 2 * 60 * 1000;
    
    default int socketTimeout() { return SOCKET_TIMEOUT; };
    default int connectTimeout() { return CONNECT_TIMEOUT; };
    default int connectionRequestTimeout() { return CONNECTION_REQUEST_TIMEOUT; }
  }

  void downloadJdk(JdkContext context, File jdkImage) throws IOException, InterruptedException;

  default void downloadJce(JdkContext context, File jceImage) throws IOException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  boolean validate(JdkContext context, File jdkImage) throws IOException, InterruptedException;

  File getImageFile(JdkContext context, File parent) throws IOException;

}
