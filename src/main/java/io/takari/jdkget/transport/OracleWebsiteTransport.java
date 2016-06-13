package io.takari.jdkget.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.compress.utils.IOUtils;

import io.takari.jdkget.Arch;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.ITransport;
import io.takari.jdkget.JdkGetter.JdkVersion;

public class OracleWebsiteTransport implements ITransport {
  
  public static final String JDK_URL_FORMAT = "http://download.oracle.com/otn-pub/java/jdk/%su%s-%s/jdk-%su%s-%s.%s";
  public static final String OTN_COOKIE = "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie";

  public boolean downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {
    
    String url = String.format(JDK_URL_FORMAT, jdkVersion.major, jdkVersion.revision, jdkVersion.buildNumber, jdkVersion.major, jdkVersion.revision, arch.getArch(), arch.getExtension());
    output.info("Downloading " + url);
    
    // Oracle does some redirects so we have to follow a couple before we win the JDK prize
    URL jdkUrl;
    int response = 0;
    HttpURLConnection connection;
    for (int retry = 0; retry < 4; retry++) {
      jdkUrl = new URL(url);
      connection = (HttpURLConnection) jdkUrl.openConnection();
      connection.setRequestProperty("Cookie", OTN_COOKIE);
      response = connection.getResponseCode();
      if (response == 200) {
        try (InputStream is = connection.getInputStream(); OutputStream os = new FileOutputStream(jdkImage)) {
          IOUtils.copy(is, os);
        }
        return true;
      } else if (response == 302) {
        url = connection.getHeaderField("Location");
      }
    }
    return false;
  }
}
