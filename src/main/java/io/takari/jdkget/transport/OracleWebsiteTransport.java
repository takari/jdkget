package io.takari.jdkget.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;

import io.takari.jdkget.Arch;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.ITransport;
import io.takari.jdkget.JdkGetter.JdkVersion;

public class OracleWebsiteTransport implements ITransport {

  public static final String ORACLE_WEBSITE = "http://download.oracle.com/otn-pub";

  public static final String JDK_URL_FORMAT = "/java/jdk/%s/jdk-%s-%s.%s";
  public static final String OTN_COOKIE = "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie";

  private String website;

  public OracleWebsiteTransport() {
    this(ORACLE_WEBSITE);
  }

  public OracleWebsiteTransport(String website) {
    this.website = website;
  }

  public boolean downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {

    String url;
    boolean cookie = true;
    if (arch == Arch.OSX_64 && jdkVersion.major == 6 && website.equals(ORACLE_WEBSITE)) {
      // for osx, jdk6* is only available from here
      url = "http://support.apple.com/downloads/DL1572/en_US/javaforosx.dmg";
      cookie = false;
    } else {
      String path = String.format(JDK_URL_FORMAT, jdkVersion.shortBuild(), jdkVersion.shortVersion(), arch.getArch(jdkVersion), arch.getExtension(jdkVersion));
      url = website + path;
    }
    output.info("Downloading " + url);

    // Oracle does some redirects so we have to follow a couple before we win the JDK prize
    URL jdkUrl;
    int response = 0;
    HttpURLConnection connection;
    for (int retry = 0; retry < 10; retry++) {
      jdkUrl = new URL(url);
      connection = (HttpURLConnection) jdkUrl.openConnection();
      if (cookie) {
        connection.setRequestProperty("Cookie", OTN_COOKIE);
      }
      response = connection.getResponseCode();
      if (response == 200) {
        try (InputStream is = connection.getInputStream(); OutputStream os = new FileOutputStream(jdkImage)) {
          IOUtils.copy(is, os);
        }
        return true;
      } else if (response == 301 || response == 302) {
        url = connection.getHeaderField("Location");
      }
    }
    return false;
  }

  public List<JdkVersion> listVersions() throws IOException {
    return new OracleVersionList().listVersions();
  }

}
