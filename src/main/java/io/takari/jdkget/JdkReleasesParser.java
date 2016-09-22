package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import io.takari.jdkget.JdkGetter.JdkVersion;
import io.takari.jdkget.JdkReleases.JdkBinary;
import io.takari.jdkget.JdkReleases.JdkRelease;

public class JdkReleasesParser {

  public JdkReleases parse(InputStream in) throws IOException {
    Document doc = new XMLParser().parse(new XMLIOSource(in));
    List<JdkRelease> releases = new ArrayList<>();
    parseDoc(doc, releases);
    releases.sort((r1, r2) -> r2.getVersion().compareTo(r1.getVersion()));
    return new JdkReleases(releases);
  }

  private void parseDoc(Document doc, List<JdkRelease> releases) {
    Element defElem = doc.getRootElement().getChild("defaults");
    String urlTemplate = defElem.getChild("url").getText();

    for (Element relElem : doc.getRootElement().getChildren("jdk")) {
      JdkVersion v = JdkVersion.parse(relElem.getAttributeValue("version"));
      boolean psu = Boolean.parseBoolean(relElem.getAttributeValue("psu"));

      JdkRelease rel = new JdkRelease(v, psu);
      releases.add(rel);

      parseBin(rel, urlTemplate, relElem.getChildren("bin"));
    }
  }

  private void parseBin(JdkRelease rel, String urlTemplate, List<Element> children) {
    for (Element binElem : children) {
      Arch cls = Arch.valueOf(binElem.getAttributeValue("cls").toUpperCase());
      String binVersion = binElem.getAttributeValue("version");
      String arch = binElem.getAttributeValue("arch");
      String ext = binElem.getAttributeValue("ext");
      String md5 = getText(binElem, "md5");
      String sha256 = getText(binElem, "sha256");
      String size = getText(binElem, "size");
      long sz = size == null ? -1 : Long.parseLong(size);
      
      JdkVersion pathVersion;
      if(binVersion != null) {
        pathVersion = JdkVersion.parse(binVersion);
      } else {
        pathVersion = rel.getVersion();
      }

      String path = path(urlTemplate, pathVersion, arch, ext);
      rel.addBinary(new JdkBinary(rel, cls, path, md5, sha256, sz));
    }
  }

  private String getText(Element e, String name) {
    Element c = e.getChild(name);
    return c == null ? null : c.getText();
  }

  private String path(String template, JdkVersion ver, String arch, String ext) {
    return template
      .replace("${version}", ver.shortVersion())
      .replace("${build}", ver.buildNumber)
      .replace("${arch}", arch)
      .replace("${ext}", ext);
  }

}
