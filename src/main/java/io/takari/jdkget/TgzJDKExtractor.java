package io.takari.jdkget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import io.takari.jdkget.JdkGetter.JdkVersion;
import io.takari.jdkget.osx.PosixModes;

public class TgzJDKExtractor implements IJdkExtractor {

  @Override
  public boolean extractJdk(JdkVersion version, File jdkImage, File outputDir, File workDir, IOutput output) throws IOException {
    
    output.info("Extracting jdk image into " + outputDir);
    
    String versionPrefix = String.format("jdk1.%s.0_%s/", version.major, version.revision);
    
    InputStream in = new FileInputStream(jdkImage);
    TarArchiveInputStream t = null;

    try {
      t = new TarArchiveInputStream(new GZIPInputStream(in));
      TarArchiveEntry te;
      while ((te = t.getNextTarEntry()) != null) {

        String entryName = te.getName();
        
        if(entryName.startsWith(versionPrefix)) {
          entryName = entryName.substring(versionPrefix.length());
        }

        File f = new File(outputDir, entryName);
        if (te.isDirectory()) {
          f.mkdirs();
        } else {
          File parent = f.getParentFile();
          if (parent != null) {
            parent.mkdirs();
          }

          if (te.isSymbolicLink()) {
            if(File.pathSeparatorChar == ';') {
              output.info("Not creating symbolic link " + entryName + " -> " + te.getLinkName());
            } else {
              Path p = f.toPath();
              Files.createSymbolicLink(p, p.getParent().resolve(te.getLinkName()));
            }
          } else {
            try(OutputStream out = new FileOutputStream(f)) {
              IOUtils.copy(t, out);
            }
            if(File.pathSeparatorChar != ';') {
              Files.setPosixFilePermissions(f.toPath(), PosixModes.intModeToPosix(te.getMode()));
            }
            f.setLastModified(te.getModTime().getTime());
          }
        }
      }
    } finally {
      in.close();
      if (t != null)
        t.close();
    }
    
    return true;
  }

}
