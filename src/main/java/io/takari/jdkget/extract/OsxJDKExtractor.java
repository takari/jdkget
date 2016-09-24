package io.takari.jdkget.extract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.codehaus.plexus.util.FileUtils;

import com.sprylab.xar.XarEntry;
import com.sprylab.xar.XarFile;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.JdkGetter.JdkVersion;
import io.takari.jdkget.Util;
import io.takari.jdkget.osx.PosixModes;
import io.takari.jdkget.osx.UnHFS;

public class OsxJDKExtractor implements IJdkExtractor {
  
  private static final String JDK6_PREFIX = "./Library/Java/JavaVirtualMachines/1.6.0.jdk/";
  
  @Override
  public boolean extractJdk(JdkVersion jdkVersion, File jdkDmg, File outputDirectory, File inProcessDirectory, IOutput output) throws IOException, InterruptedException {
    
    output.info("Extracting osx dmg image into " + outputDirectory);
    
    // DMG <-- XAR <-- GZ <-- CPIO
    UnHFS.unhfs(jdkDmg, inProcessDirectory);
    
    List<File> payloads = new ArrayList<>();

    List<File> files = FileUtils.getFiles(inProcessDirectory, "**/*.pkg", null, true);
    // validate
    
    File jdkPkg = files.get(0);
    XarFile xarFile = new XarFile(jdkPkg);
    for (XarEntry entry : xarFile.getEntries()) {
      Util.checkInterrupt();
      String name = entry.getName();
      if (!entry.isDirectory() && (name.startsWith("jdk") || name.startsWith("JavaForOSX") || name.startsWith("JavaEssentials") || name.startsWith("JavaMDNS")) && entry.getName().endsWith("Payload")) {
        File file = new File(inProcessDirectory, name);
        File parentFile = file.getParentFile();
        if(parentFile.isFile()) {
          parentFile = new File(parentFile.getParentFile(), parentFile.getName() + ".tmp");
          file = new File(parentFile, file.getName());
        }
        parentFile.mkdirs();
        try (InputStream is = entry.getInputStream(); OutputStream os = new FileOutputStream(file)) {
          Util.copyInterruptibly(is, os);
        }
        payloads.add(file);
      }
    }

    for(File jdkGz: payloads) {
      Util.checkInterrupt();
      File cpio = new File(inProcessDirectory, "temp" + System.currentTimeMillis() + ".cpio");
      try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(jdkGz)); FileOutputStream os = new FileOutputStream(cpio)) {
        Util.copyInterruptibly(is, os);
      }

      // https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt
      try (ArchiveInputStream is = new CpioArchiveInputStream(new FileInputStream(cpio))) {
        CpioArchiveEntry e;
        while ((e = (CpioArchiveEntry) is.getNextEntry()) != null) {
          Util.checkInterrupt();
          if (!e.isDirectory()) {
            String name = e.getName();
            File jdkFile = new File(outputDirectory, name);
            jdkFile.getParentFile().mkdirs();
            if (e.isRegularFile()) {
              try (OutputStream os = new FileOutputStream(jdkFile)) {
                Util.copyInterruptibly(is, os);
              }
            } else if (e.isSymbolicLink()) {
              try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                Util.copyInterruptibly(is, os);
                String target = new String(os.toByteArray());
                if(target.startsWith(JDK6_PREFIX)) {
                  target = target.substring(JDK6_PREFIX.length());
                }
                
                if(File.pathSeparatorChar == ';') {
                  output.info("Not creating symbolic link " + e.getName() + " -> " + target);
                } else {
                  Files.createSymbolicLink(jdkFile.toPath(), Paths.get(target));
                }
              }
            }
            // The lower 9 bits specify read/write/execute permissions for world, group, and user following standard POSIX conventions.
            if(File.pathSeparatorChar != ';') {
              int mode = (int) e.getMode() & 0000777;
              Files.setPosixFilePermissions(jdkFile.toPath(), PosixModes.intModeToPosix(mode));
            }
          }
        }
      }
    }
    
    return true;
  }

}
