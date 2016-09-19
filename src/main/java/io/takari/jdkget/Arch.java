package io.takari.jdkget;

import java.util.Locale;

import io.takari.jdkget.JdkGetter.JdkVersion;

public enum Arch {
  OSX_64("macosx-x64", "dmg"),
  NIX_32("linux-i586", "tar.gz") {
    @Override
    public String getExtension(JdkVersion v) {
      return v.major > 6 ? super.getExtension(v) : "bin";
    }
  },
  NIX_64("linux-x64", "tar.gz") {
    @Override
    public String getArch(JdkVersion v) {
      if(v.major == 6) { 
        if(v.revision < 4) {
          return "linux-amd64";
        }
      }
      return super.getArch(v);
    }
    
    @Override
    public String getExtension(JdkVersion v) {
      return v.major > 6 ? super.getExtension(v) : "bin";
    }
  },
  WIN_32("windows-i586", "exe") {
    @Override
    public String getArch(JdkVersion v) {
      if(v.major == 6 && v.revision > 0 && v.revision < 14) {
        return "windows-i586-p";
      }
      return super.getArch(v);
    }
  },
  WIN_64("windows-x64", "exe") {
    @Override
    public String getArch(JdkVersion v) {
      if(v.major == 6) { 
        if(v.revision < 4) {
          return "windows-amd64";
        }
        if(v.revision > 11 && v.revision < 14) {
          return "windows-x64-p";
        }
      }
      return super.getArch(v);
    }
  },
  SOL_64("solaris-x64", "tar.gz") {
    @Override
    public String getExtension(JdkVersion v) {
      return v.major > 6 ? super.getExtension(v) : "tar.Z";
    }
  },
  SOL_SPARC("solaris-sparcv9", "tar.gz") {
    @Override
    public String getExtension(JdkVersion v) {
      return v.major > 6 ? super.getExtension(v) : "tar.Z";
    }
  };
  
  private String arch;
  private String extension;

  private Arch(String arch, String extension) {
    this.arch = arch;
    this.extension = extension;
  }
  
  public String getArch(JdkVersion v) {
    return arch;
  }
  
  public String getExtension(JdkVersion v) {
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
