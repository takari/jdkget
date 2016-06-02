package io.takari.jdkget.win32;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dorkbox.cabParser.CabException;
import dorkbox.cabParser.CabParser;
import dorkbox.cabParser.CabStreamSaver;
import dorkbox.cabParser.structure.CabFileEntry;
import dorkbox.peParser.PE;
import dorkbox.peParser.headers.resources.ResourceDataEntry;
import dorkbox.peParser.headers.resources.ResourceDirectoryEntry;
import dorkbox.peParser.headers.resources.ResourceDirectoryHeader;
import dorkbox.peParser.misc.DirEntry;
import dorkbox.peParser.types.ImageDataDir;
import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.JdkGetter.JdkVersion;

public class WindowsJDKExtractor implements IJdkExtractor {
  
  private static final Set<String> SKIP_DIRS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "Icon", "Bitmap")));
  
  @Override
  public boolean extractJdk(JdkVersion jdkVersion, File jdkImage, File outputDir, File workDir, IOutput output) throws IOException {
    
    output.info("Extracting tools.zip from install executable into " + outputDir);
    
    // <= 1.7: PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)
    // > 1.7:  PE EXE <- PE EXE <- CAB <- tools.zip (some jars are pack200'd as .pack)
    
    File tmptools = new File(workDir, "tools-" + System.currentTimeMillis() + ".zip");
    if(scanPE(jdkImage, tmptools, workDir)) {
      try {
        extractTools(tmptools, outputDir);
        return true;
      } finally {
        if(!tmptools.delete()) {
          tmptools.deleteOnExit();
        }
      }
    }
    return false;
  }
  
  private void extractTools(File toolZip, File output) throws IOException {
    output.mkdirs();
    
    ZipFile zip = new ZipFile(toolZip);
    try {
      
      Enumeration<? extends ZipEntry> en = zip.entries();
      while(en.hasMoreElements()) {
        ZipEntry e = en.nextElement();
        
        boolean unpack200 = false;
        String name = e.getName();
        
        if(name.endsWith(".pack")) {
          name = name.substring(0, name.length() - 5) + ".jar";
          unpack200 = true;
        }
        
        File f = new File(output, name);
        if(e.isDirectory()) {
          
          f.mkdirs();
          
        } else {
          
          f.createNewFile();
          OutputStream out = new FileOutputStream(f);
          InputStream in = zip.getInputStream(e);
          
          try {
            if(unpack200) {
              Pack200.newUnpacker().unpack(in, new JarOutputStream(out));
            } else {
              byte[] buf = new byte[1024*128]; // 128k
              int l;
              while((l = in.read(buf)) != -1) {
                out.write(buf, 0, l);
              }
            }
          } finally {
            out.close();
            in.close();
          }
          
        }
      }
      
    } finally {
      zip.close();
    }
  }

  private boolean scanPE(File f, File outputDir, File workDir) throws IOException {
    PE pe;
    try {
      pe = new PE(f.getCanonicalPath());
    } catch(Exception e) {
      return false;
    }
    
    if(pe.isPE()) {
      for (ImageDataDir entry : pe.optionalHeader.tables) {
        if (entry.getType() == DirEntry.RESOURCE) {
          
          ResourceDirectoryHeader root = (ResourceDirectoryHeader) entry.data;
          if(scanPEDir(pe, root, outputDir, workDir)) {
            return true;
          }
          
        }
      }
      
    }
    return false;
  }

  private boolean scanPEDir(PE pe, ResourceDirectoryHeader dir, File outputDir, File workDir) throws IOException {
    for(ResourceDirectoryEntry entry: dir.entries) {
      if(entry.isDirectory) {
        if(SKIP_DIRS.contains(entry.NAME.get())) {
          continue;
        }
        if(scanPEDir(pe, entry.directory, outputDir, workDir)) {
          return true;
        }
      } else {
        if(scanPEEntry(pe, entry.resourceDataEntry, outputDir, workDir)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean scanPEEntry(PE pe, ResourceDataEntry resourceDataEntry, File outputDir, File workDir) throws IOException {
    
    byte[] data = resourceDataEntry.getData(pe.fileBytes);
    if(scanCab(outputDir, data)) {
      return true;
    }
    
    // try pe
    File f = new File(workDir, "cabextract-" + System.currentTimeMillis() + ".tmp");
    f.createNewFile();
    try {
      OutputStream out = new FileOutputStream(f);
      try {
        out.write(data);
      } finally {
        out.close();
      }
      if(scanPE(f, outputDir, workDir)) {
        return true;
      }
    } finally {
      if(!f.delete()) {
        f.deleteOnExit();
      }
    }
    
    return false;
  }

  private boolean scanCab(final File output, byte[] data) throws IOException {
    final boolean[] res = new boolean[]{ false };
    InputStream in = new ByteArrayInputStream(data);
    try {
      new CabParser(in, new CabStreamSaver() {
        public boolean saveReservedAreaData(byte[] data, int dataLength) {
          return false;
        }
        public OutputStream openOutputStream(CabFileEntry entry) {
          if(res[0]) return null;
          
          if(entry.getName().equals("tools.zip")) {
            res[0] = true;
            try {
              output.createNewFile();
              return new FileOutputStream(output);
            } catch (IOException e) {
              return null;
            }
          }
          return null;
        }
        public void closeOutputStream(OutputStream os, CabFileEntry entry) {
          try{ os.close(); } catch(IOException e){}
        }
      }).extractStream();
    } catch (CabException e) {
      // not a cab file, probably
    }
    return res[0];
  }
}
