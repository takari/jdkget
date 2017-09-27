package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class JdkReleasesParser {

  public JdkReleases parse(InputStream in) throws IOException {
    Document doc = new XMLParser().parse(new XMLIOSource(in));
    JdkReleases.Builder builder = JdkReleases.newBuilder();
    parseDoc(doc, builder);
    return builder.build();
  }

  private void parseDoc(Document doc, JdkReleases.Builder builder) {
    Element defElem = doc.getRootElement().getChild("defaults");
    String urlTemplate = defElem.getChild("url").getText();

    for (Element jceElem : doc.getRootElement().getChildren("jce")) {
      String ver = jceElem.getAttributeValue("version");
      String url = getText(jceElem, "url");
      builder.addJCE(Integer.parseInt(ver), url);
    }

    for (Element relElem : doc.getRootElement().getChildren("jdk")) {
      String v = relElem.getAttributeValue("version");
      boolean psu = Boolean.parseBoolean(relElem.getAttributeValue("psu"));
      String url = getText(relElem, "url");

      if (url == null) {
        url = urlTemplate;
      }

      if (psu) {
        builder.setPSU(v);
      }

      parseBin(v, url, relElem.getChildren("bin"), builder);
    }
  }

  private void parseBin(String ver, String urlTemplate, List<Element> children, JdkReleases.Builder builder) {
    for (Element binElem : children) {
      Arch cls = Arch.valueOf(binElem.getAttributeValue("cls").toUpperCase());
      String binVersion = binElem.getAttributeValue("version");
      String arch = binElem.getAttributeValue("arch");
      String ext = binElem.getAttributeValue("ext");
      String md5 = getText(binElem, "md5");
      String sha256 = getText(binElem, "sha256");
      String size = getText(binElem, "size");
      String url = getText(binElem, "url");
      long sz = size == null ? -1 : Long.parseLong(size);

      String pathVersion;
      if (binVersion != null) {
        pathVersion = binVersion;
      } else {
        pathVersion = ver;
      }

      if (url == null) {
        url = urlTemplate;
      }

      String path = path(url, JdkVersion.parse(pathVersion), arch, ext);
      builder.addBinary(ver, cls, path, md5, sha256, sz);
    }
  }

  private String getText(Element e, String name) {
    Element c = e.getChild(name);
    return c == null ? null : c.getText();
  }

  private String path(String template, JdkVersion ver, String arch, String ext) {
    return template //
        .replace("${version}", ver.shortVersion()) //
        .replace("${build}", ver.buildNumber) //
        .replace("${arch}", arch) //
        .replace("${ext}", ext);
  }

}
