package io.takari.jdkget;

import java.util.Locale;

public enum Arch {
  OSX_64("macosx-x64", "dmg"),
  NIX_32("linux-i586", "tar.gz"),
  NIX_64("linux-x64", "tar.gz"),
  WIN_32("windows-i586", "exe"),
  WIN_64("windows-x64", "exe"),
  SOL_64("solaris-x64", "tar.gz"),
  SOL_SPARC("solaris-sparcv9", "tar.gz");
  
  private String arch;
  private String extension;

  private Arch(String arch, String extension) {
    this.arch = arch;
    this.extension = extension;
  }
  
  public String getArch() {
    return arch;
  }
  
  public String getExtension() {
    return extension;
  }

  public static Arch autodetect() {
    String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    
    boolean sixtyFour;
    
    if(arch.contains("amd64")) sixtyFour = true;
    else if(arch.contains("86_64")) sixtyFour = true;
    else if(arch.contains("86")) sixtyFour = false;
    else if(arch.contains("sparc")) sixtyFour = false; // well, technically it's 64
    //else if(arch.contains("ia64"))   return Itanium; // not supported by oracle since 6u22
    else {
      throw new IllegalStateException("Unsupported architecture " + arch);
    }
    
    if(os.contains("linux")) {
      return sixtyFour ? NIX_64 : NIX_32;
    } else if(os.contains("windows")) {
      return sixtyFour ? WIN_64 : WIN_32;
    } else if(os.contains("mac")) {
      return OSX_64;
    } else if(arch.contains("sun") || arch.contains("solaris")) {
      return sixtyFour ? SOL_64 : SOL_SPARC;
    } else {
      throw new IllegalStateException("Unsupported architecture " + arch);
    }
  }
}
