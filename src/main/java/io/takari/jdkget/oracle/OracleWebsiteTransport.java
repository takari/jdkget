package io.takari.jdkget.oracle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import io.takari.jdkget.IOutput;
import io.takari.jdkget.ITransport;
import io.takari.jdkget.JdkGetter;
import io.takari.jdkget.Util;
import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.JdkBinary;

public class OracleWebsiteTransport implements ITransport {

  public static final String ORACLE_WEBSITE = "http://download.oracle.com/otn-pub";

  public static final String JDK_URL_FORMAT = "/java/jdk/%s/jdk-%s-%s.%s";

  private final String website;
  private final String otnUsername;
  private final String otnPassword;
  private volatile BasicCookieStore cookieStore;

  public OracleWebsiteTransport() {
    this(ORACLE_WEBSITE);
  }

  public OracleWebsiteTransport(String website) {
    this(website, null, null);
  }

  public OracleWebsiteTransport(String website, String otnUsername, String otnPassword) {
    this.website = website == null ? ORACLE_WEBSITE : website;
    this.otnUsername = otnUsername;
    this.otnPassword = otnPassword;
  }

  @Override
  public void downloadJdk(JdkGetter context, JdkBinary bin, File jdkImage) throws IOException, InterruptedException {
    doDownload(context, website + "/" + bin.getPath(), jdkImage);
  }

  @Override
  public void downloadJce(JdkGetter context, JCE jce, File jceImage) throws IOException, InterruptedException {
    if (jce == null) {
      throw new IllegalStateException("No JCE provided");
    }

    doDownload(context, website + "/" + jce.getPath(), jceImage);
  }

  private void doDownload(JdkGetter context, final String url, File target) throws IOException, InterruptedException {
    IOutput output = context.getLog();
    boolean echo = !context.isSilent();
    if (echo) {
      output.info("Downloading " + cleanUrl(url));
    }

    BasicCookieStore cookieStore = initCookieStore();

    RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout(context.getSocketTimeout())
        .setConnectTimeout(context.getConnectTimeout())
        .setConnectionRequestTimeout(context.getConnectionRequestTimeout())
        .build();

    CloseableHttpClient cl = HttpClientBuilder.create()
        .setDefaultRequestConfig(requestConfig)
        .setDefaultCookieStore(cookieStore)
        .disableRedirectHandling()
        // .setUserAgent("curl/7.47.0")
        // User Agent String of Safari
        .setUserAgent("Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) "
            + "AppleWebKit/537.51.1 (KHTML, like Gecko) "
            + "CriOS/30.0.1599.12 Mobile/11A465 Safari/8536.25 "
            + "(3B92C18B-D9DE-4CB7-A02A-22FD2AF17C8F)")
        .build();

    boolean hasOtnCredentials = StringUtils.isNotBlank(otnUsername) && StringUtils.isNotBlank(otnPassword);

    HttpRequestBase req = new HttpGet(url);

    // Oracle does some redirects so we have to follow a couple before we win the JDK prize
    int retries = 20;
    for (int retry = 0; retry < retries; retry++) {

      CloseableHttpResponse res = cl.execute(req);
      try {
        int code = res.getStatusLine().getStatusCode();
        String msg = res.getStatusLine().getReasonPhrase();

        boolean shouldTryLogin = hasOtnCredentials && req.getURI().getHost().equals("login.oracle.com");

        if (code == 401 && shouldTryLogin) {
          req = createLoginBasic(URI.create(res.getFirstHeader("Location").getValue()), otnUsername, otnPassword);
          if (echo) {
            output.info("Basic authorizing on " + cleanUrl(req.getURI().toString()));
          }
        }
        if (code == 200 && shouldTryLogin) {
          req = createLoginPost(req.getURI(), res);
          if (echo) {
            output.info("Authorizing on " + cleanUrl(req.getURI().toString()));
          }
        } else if (code == 200) {
          downloadResponse(context, res, target);
          return;
        } else if (code == 301 || code == 302) {
          String newUrl = res.getFirstHeader("Location").getValue();
          if (echo) {
            output.info("Redirecting to " + cleanUrl(newUrl));
          }
          req = new HttpGet(newUrl);
        } else if (code == 404) {
          if (hasOtnCredentials && url.contains("/otn-pub/")) {
            if (echo) {
              output.info("Server responded with " + code + ": " + msg + ", retrying with OTN credentials");
            }
            req = new HttpGet(url.replace("/otn-pub/", "/otn/"));
          } else {
            output.error("Server responded with " + code + ": " + msg);
            throw new IOException("Could not download jdk");
          }
        } else {
          output.error("Server responded with " + code + ": " + msg + ", retrying");
          req = new HttpGet(req.getURI());
        }
      } finally {
        res.close();
      }
    }

    throw new IOException("Could not download jdk after " + retries + " attempts");
  }

  private BasicCookieStore initCookieStore() {
    if (cookieStore == null) {
      synchronized (this) {
        if (cookieStore == null) {
          cookieStore = new BasicCookieStore();
          cookieStore.addCookie(new BasicClientCookie("oraclelicense", "accept-securebackup-cookie"));
          cookieStore.addCookie(new BasicClientCookie("gpw_e24", "http%3A%2F%2Fwww.oracle.com"));
          cookieStore.getCookies().forEach(c -> {
            BasicClientCookie bc = (BasicClientCookie) c;
            bc.setDomain(".oracle.com");
            bc.setPath("/");
            bc.setAttribute(ClientCookie.PATH_ATTR, bc.getPath());
            bc.setAttribute(ClientCookie.DOMAIN_ATTR, bc.getDomain());
          });
        }
      }
    }
    return cookieStore;
  }

  private static String cleanUrl(String url) {
    int q = url.indexOf('?');
    return q != -1 ? url.substring(0, q) : url;
  }

  private void downloadResponse(JdkGetter context, HttpResponse res, File target)
      throws IOException, InterruptedException, FileNotFoundException {
    Header contentLength = res.getFirstHeader("Content-Length");
    long totalHint = -1;
    if (contentLength != null) {
      try {
        totalHint = Long.parseLong(contentLength.getValue());
      } catch (NumberFormatException e) {
      }
    }

    try (InputStream is = res.getEntity().getContent(); OutputStream os = new FileOutputStream(target)) {
      IOutput output = context.isSilent() ? IOutput.NULL_OUTPUT : context.getLog();
      Util.copyWithProgress(is, os, totalHint, output);
    }
  }

  private HttpRequestBase createLoginPost(URI uri, HttpResponse res) throws IOException {
    String pageData;
    HttpEntity entity = res.getEntity();
    String enc = entity.getContentEncoding() == null ? null : entity.getContentEncoding().getValue();
    try (InputStream content = entity.getContent()) {
      pageData = IOUtils.toString(content, enc);
    }

    int formStart = pageData.indexOf("<form ");
    if (formStart == -1) {
      throw new IOException("No form found at login page " + uri);
    }
    int formStartEnd = pageData.indexOf(">", formStart);
    if (formStartEnd == -1) {
      throw new IOException("Form begin tag not closed at login page " + uri);
    }
    int formEnd = pageData.indexOf("</form>", formStartEnd);
    if (formEnd == -1) {
      throw new IOException("Form end tag not found at login page " + uri);
    }

    String action = findAttr(pageData.substring(formStart, formStartEnd), "action");
    if (action.startsWith("/")) {
      String scheme = uri.getScheme();
      int p = uri.getPort();
      String port = p >= 0 ? ":" + p : "";
      action = scheme + "://" + uri.getHost() + port + action;
    }
    HttpPost post = new HttpPost(action);

    List<NameValuePair> formparams = new ArrayList<NameValuePair>();

    int nextInput = formStartEnd + 1;
    while (nextInput < formEnd) {
      int inputStart = pageData.indexOf("<input ", nextInput);
      if (inputStart == -1) {
        break;
      }
      int inputEnd = pageData.indexOf('>', inputStart);
      if (inputEnd == -1) {
        break;
      }


      String n = findAttr(pageData.substring(inputStart, inputEnd), "name");
      if (n != null) {
        String v = findAttr(pageData.substring(inputStart, inputEnd), "value");

        if (n.equals("ssousername")) {
          v = otnUsername;
        } else if (n.equals("password")) {
          v = otnPassword;
        }

        formparams.add(new BasicNameValuePair(n, v));
      }

      nextInput = inputEnd + 1;
    }
    post.setEntity(new UrlEncodedFormEntity(formparams, Consts.UTF_8));
    return post;
  }

  private static String findAttr(String data, String name) {
    String pref = name + "=\"";
    int valueStart = data.indexOf(pref);
    if (valueStart == -1) {
      return null;
    }
    int valueEnd = data.indexOf('"', valueStart + pref.length());
    return StringEscapeUtils.unescapeHtml4(data.substring(valueStart + pref.length(), valueEnd));
  }

  private HttpRequestBase createLoginBasic(URI uri, String username, String password) throws IOException {
    HttpGet req = new HttpGet(uri);
    String auth = username + ":" + password;
    String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes("utf-8"));
    req.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
    return req;
  }

  @Override
  public boolean validate(JdkGetter context, JdkBinary bin, File jdkImage) throws IOException, InterruptedException {

    IOutput output = context.getLog();

    int checks = 0;
    int failed = 0;

    if (bin.getSha256() != null) {
      checks++;
      String fileHash = hash(jdkImage, Hashing.sha256());
      if (!bin.getSha256().equals(fileHash)) {
        failed++;
        output.error("File sha256 `" + fileHash + "` differs from `" + bin.getSha256() + "`");
      }
    }
    if (bin.getMd5() != null) {
      checks++;
      String fileHash = hash(jdkImage, Hashing.md5());
      if (!bin.getMd5().equals(fileHash)) {
        failed++;
        output.error("File md5 `" + fileHash + "` differs from `" + bin.getMd5() + "`");
      }
    }
    if (bin.getSize() != -1) {
      checks++;
      if (bin.getSize() != jdkImage.length()) {
        failed++;
        output.error("File size `" + jdkImage.length() + "` differs from `" + bin.getSize() + "`");
      }
    }

    if (checks != 0 && failed > 0) {
      return false;
    }
    return true;
  }

  private static String hash(File f, HashFunction hf) throws IOException {
    Hasher h = hf.newHasher();
    try (InputStream in = new FileInputStream(f)) {
      byte[] buf = new byte[8192];
      int l;
      while ((l = in.read(buf)) != -1) {
        h.putBytes(buf, 0, l);
      }
    }
    return h.hash().toString();
  }

}
