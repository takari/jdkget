package io.takari.jdkget;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.takari.jdkget.JavaReleasesData.BinaryData;
import io.takari.jdkget.JavaReleasesData.DefaultsData;
import io.takari.jdkget.JdkReleases.Builder;
import io.takari.jdkget.JdkReleases.JavaReleaseType;

public class JdkReleasesParser {
  public JdkReleases parse(InputStream in) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JavaReleasesData jrd = mapper.readValue(in, JavaReleasesData.class);
    
    JdkReleases.Builder builder = JdkReleases.newBuilder();
    buildReleases(jrd, builder);
    return builder.build();
  }
  
  private void buildReleases(JavaReleasesData jrd, Builder builder) {
    if(jrd == null) { return; }

    DefaultsData defaults = jrd.getDefaults();

    if(jrd.getJces() != null) {
      jrd.getJces().stream()
        .forEach(j -> 
          builder.addJCE(Integer.parseInt(j.getVersion()), j.getUrl())
         );
    }

    if(jrd.getReleases() == null) { return; }
    
    jrd.getReleases().forEach(r -> 
    {
      String v = r.getVersion();
      boolean psu = r.getPsu() == null ? false : r.getPsu().booleanValue();
      String url = r.getUrl() == null ? defaults.getUrl() : r.getUrl();

      if (psu) {
        builder.setPSU(v);
      }

      buildReleaseBinary(v, url, r.getJdk(), builder, defaults, JavaReleaseType.JDK.getName());
      buildReleaseBinary(v, url, r.getJre(), builder, defaults, JavaReleaseType.JRE.getName());
      buildReleaseBinary(v, url, r.getServerJre(), builder, defaults, JavaReleaseType.SERVERJRE.getName());
    });

    for (String unp : jrd.getUnpackable()) {
      builder.addUnpackable(unp);
    }
  }
  
  private void buildReleaseBinary(String ver, String urlTemplate, List<BinaryData> bins, Builder builder,
      DefaultsData defaults, String type) {
    if(bins == null) { return; }
    
    bins.forEach(b -> {
      String arch = b.getArch();

      String clsName = defaults.getArchCls().get(arch);
      if(clsName == null) {
        throw new IllegalStateException("No classifier for arch " + arch);
      }
      Arch cls = Arch.valueOf(clsName.toUpperCase());
      String binVersion = b.getVersion() == null ? ver : b.getVersion();
      String typeName = b.getTypeName() == null ? defaults.getTypeName().get(type) : b.getTypeName();
      String ext = b.getExt();
      String md5 = b.getMd5();
      String sha256 = b.getSha256();
      long size = b.getSize() == null ? -1 : b.getSize();
      String url = b.getUrl() == null ? urlTemplate : b.getUrl();
      
      String path = path(url, typeName, JdkVersion.parse(binVersion), arch, ext);

      String descriptor = arch + "." + ext;
      builder.addBinary(ver, type, cls, descriptor, path, md5, sha256, size);
    });
  }

  private String path(String template, String typeName, JdkVersion ver, String arch, String ext) {
    return template //
        .replace("${typeName}", typeName) //
        .replace("${version}", ver.shortVersion()) //
        .replace("${build}", ver.buildNumber) //
        .replace("${arch}", arch) //
        .replace("${ext}", ext);
  }
}
