package io.takari.jdkget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "jdks")
public class JavaReleasesData {
  public static class JceData {
    @JacksonXmlProperty(isAttribute = true)
    private String version;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    private String url;
  }

  public static class DefaultsData {
    private String url;
    private Map<String,String> typeName;
    private Map<String,String> archCls;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public Map<String, String> getTypeName() {
      if(typeName == null) {
        typeName = new HashMap<String,String>();
      }
      
      return typeName;
    }

    public void setTypeName(Map<String, String> typeName) {
     this.typeName = typeName;
    }

    public Map<String,String> getArchCls() {
      if(archCls == null) {
        archCls = new HashMap<String,String>();
      }
      
      return archCls;
    }

    public void setArchCls(Map<String,String> archCls) {
      this.archCls = archCls;
    }
  }

  public static class BinaryData {
    @JacksonXmlProperty(isAttribute = true)
    private String version;

    @JacksonXmlProperty(isAttribute = true)
    private String cls;

    @JacksonXmlProperty(isAttribute = true)
    private String arch;

    @JacksonXmlProperty(isAttribute = true)
    private String ext;

    private String sha256;

    private String md5;

    private Long size;
    
    private String url;

    private String typeName;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public Long getSize() {
      return size;
    }

    public void setSize(Long size) {
      this.size = size;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getCls() {
      return cls;
    }

    public void setCls(String cls) {
      this.cls = cls;
    }

    public String getArch() {
      return arch;
    }

    public void setArch(String arch) {
      this.arch = arch;
    }

    public String getExt() {
      return ext;
    }

    public void setExt(String ext) {
      this.ext = ext;
    }

    public String getSha256() {
      return sha256;
    }

    public void setSha256(String sha256) {
      this.sha256 = sha256;
    }

    public String getMd5() {
      return md5;
    }

    public void setMd5(String md5) {
      this.md5 = md5;
    }

    public void setTypeName(String typeName) {
      this.typeName = typeName;
    }
    
    public String getTypeName() {
      return typeName;
    }
  }
  
  public static class JavaReleaseData {
    @JacksonXmlProperty(isAttribute = true)
    private String version;

    @JacksonXmlProperty(isAttribute = true)
    private Boolean psu;

    @JacksonXmlProperty(isAttribute = true)
    private String ext;

    private String url;

    @JsonProperty("jdk")
    @JacksonXmlProperty(localName = "bin")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BinaryData> jdk;
    
    @JsonProperty("jre")
    @JacksonXmlProperty(localName = "jre")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BinaryData> jre;

    @JsonProperty("serverjre")
    @JacksonXmlProperty(localName = "serverjre")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BinaryData> serverJre;
    
    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public Boolean getPsu() {
      return psu;
    }

    public void setPsu(Boolean psu) {
      this.psu = psu;
    }

    public String getExt() {
      return ext;
    }

    public void setExt(String ext) {
      this.ext = ext;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public List<BinaryData> getJdk() {
      return jdk;
    }

    public void setJdk(List<BinaryData> jdk) {
      this.jdk = jdk;
    }

    public List<BinaryData> getJre() {
      return jre;
    }

    public void setJre(List<BinaryData> jre) {
      this.jre = jre;
    }

    public List<BinaryData> getServerJre() {
      return serverJre;
    }

    public void setServerJre(List<BinaryData> serverJre) {
      this.serverJre = serverJre;
    }
  }

  @JsonProperty("jce")
  @JacksonXmlProperty(localName = "jce")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<JceData> jces;

  private DefaultsData defaults;

  @JsonProperty("java")
  @JacksonXmlProperty(localName = "jdk")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<JavaReleaseData> releases;

  public List<JceData> getJces() {
    return jces;
  }

  public void setJces(List<JceData> jces) {
    this.jces = jces;
  }

  public DefaultsData getDefaults() {
    return defaults;
  }

  public void setDefaults(DefaultsData defaults) {
    this.defaults = defaults;
  }

  public List<JavaReleaseData> getReleases() {
    return releases;
  }

  public void setReleases(List<JavaReleaseData> releases) {
    this.releases = releases;
  }

}
