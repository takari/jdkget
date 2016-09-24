package io.takari.jdkget.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.Util;
import io.takari.jdkget.JdkGetter.JdkVersion;
import io.takari.jdkget.osx.PosixModes;

public abstract class AbstractTarJDKExtractor implements IJdkExtractor {

  protected abstract InputStream wrap(InputStream in) throws IOException;

  @Override
  public boolean extractJdk(JdkVersion version, File jdkImage, File outputDir, File workDir, IOutput output) throws IOException, InterruptedException {

    output.info("Extracting jdk image into " + outputDir);

    String versionPrefix = "jdk" + version.longVersion();

    try (InputStream in = new FileInputStream(jdkImage)) {
      TarArchiveInputStream t = new TarArchiveInputStream(wrap(in));
      TarArchiveEntry te;
      while ((te = t.getNextTarEntry()) != null) {

        Util.checkInterrupt();

        String entryName = te.getName();

        if (entryName.startsWith(versionPrefix)) {
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
            if (File.pathSeparatorChar == ';') {
              output.info("Not creating symbolic link " + entryName + " -> " + te.getLinkName());
            } else {
              Path p = f.toPath();
              Files.createSymbolicLink(p, p.getParent().resolve(te.getLinkName()));
            }
          } else {
            try (OutputStream out = new FileOutputStream(f)) {
              Util.copyInterruptibly(t, out);
            }
            if (File.pathSeparatorChar != ';') {
              int mode = (int) te.getMode() & 0000777;
              Files.setPosixFilePermissions(f.toPath(), PosixModes.intModeToPosix(mode));
            }
            f.setLastModified(te.getModTime().getTime());
          }
        }
      }
    }

    return true;
  }

}
