package io.takari.jdkget.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.takari.jdkget.JdkGetter.JdkVersion;

public class OracleVersionList {
  
  private static final String baseUrl = "http://www.oracle.com";
  private static final String initialPath = "/technetwork/java/javase/downloads/index.html";
  
  private static final Pattern downloadsPathPattern = Pattern.compile(q("/technetwork/java/javase/downloads/") + "[^\\-]+" + q("-downloads-") + "\\w+" + q(".html"));
  private static final Pattern archiveIndexPathPattern = Pattern.compile(q("/technetwork/java/javase/archive-") + "\\w+" + q(".html"));
  
  // /technetwork/java/javase/downloads/java-archive-javase8-2177648.html
  // /technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html
  // /technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html
  private static final Pattern archivePathPattern = Pattern.compile(q("/technetwork/java/javase/downloads/java-archive-") + "[^\\.]+" + q(".html"));
  
  
  // downloads['jdk-8u91-oth-JPR']['files']['jdk-8u91-linux-i586.tar.gz'] = { "title":"Linux x86", "size":"174.92 MB","filepath":"http://download.oracle.com/otn-pub/java/jdk/8u91-b14/jdk-8u91-linux-i586.tar.gz"};
  // downloads['jdk-8u77-oth-JPR']['files']['jdk-8u77-linux-i586.tar.gz'] = { "title":"Linux x86", "size":"174.92 MB","filepath":"http://download.oracle.com/otn/java/jdk/8u77-b03/jdk-8u77-linux-i586.tar.gz"};
  // downloads['jdk-6u45-oth-JPR']['files']['jdk-6u45-linux-i586.bin'] = { "title":"Linux x86", "size":"68.47 MB","filepath":"http://download.oracle.com/otn/java/jdk/6u45-b06/jdk-6u45-linux-i586.bin"};
  private static final Pattern searchPattern = Pattern.compile(
      q("downloads['") + "[^\\']+" + 
      q("']['files']['jdk-") + "[^\\']+" + 
      q("-linux-i586.") + "(tar\\.gz|bin)" + q("'] = { ") + "[^\\}]+" + 
      q("\"filepath\":\"http://download.oracle.com/otn") + "(-pub)?" + q("/java/jdk/") + "([^\\/]+)" + 
      q("/jdk-") + "[^\\-]+" + 
      q("-linux-i586.") + "(tar\\.gz|bin)" + q("\"}"));
  
  private static String q(String s) {
    return Pattern.quote(s);
  }
  
  public List<JdkVersion> listVersions() throws IOException {
    
    List<JdkVersion> versions = new ArrayList<>();
    
    Links links = processIndex();
    
    if(links.downloads != null) {
      fetchVersions(links.downloads, versions);
    }
    if(links.archive != null) {
      for(String a: links.archive) {
        fetchVersions(a, versions);
      }
    }
    
    return versions;
  }
  
  private void fetchVersions(String link, List<JdkVersion> versions) throws IOException {
    fetch(baseUrl + link, l -> {
      findAll(l, searchPattern, 3, v -> versions.add(JdkVersion.parse(v)));
    });
  }

  private Links processIndex() throws IOException {
    Links links = new Links();
    String[] archive = new String[1];
    
    fetch(baseUrl + initialPath, l -> {
      if(links.downloads == null) links.downloads = find(l, downloadsPathPattern, 0);
      if(archive[0] == null) archive[0] = find(l, archiveIndexPathPattern, 0);
    });
    
    if(archive[0] != null) {
      links.archive = new ArrayList<>();
      
      fetch(baseUrl + archive[0], l -> {
        findAll(l, archivePathPattern, 0, p -> links.archive.add(p));
      });
      
    }
    
    return links;
  }
  
  private String find(String l, Pattern p, int group) {
    Matcher m = p.matcher(l);
    if(m.find()) {
      return m.group(group);
    }
    return null;
  }
  
  private void findAll(String l, Pattern p, int group, Consumer<String> c) {
    Matcher m = p.matcher(l);
    while(m.find()) {
      c.accept(m.group(group));
    }
  }
  
  protected void fetch(String url, Consumer<String> c) throws IOException {
    URL theUrl = new URL(url);
    HttpURLConnection connection = (HttpURLConnection) theUrl.openConnection();
    int response = connection.getResponseCode();
    if (response == 200) {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
        
        String l;
        while((l = br.readLine()) != null) {
          c.accept(l);
        }
      }
    } else {
      throw new IllegalStateException("Response code " + response);
    }
  }
  
  static class Links {
    String downloads;
    List<String> archive;
  }
}
