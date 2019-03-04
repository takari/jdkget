package io.takari.jdkget.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.takari.jdkget.Arch;

public class JdkRelease implements Serializable {
  private static final long serialVersionUID = 1L;

  private final JdkVersion version;
  private final boolean psu;
  final Map<BinaryType, Map<Arch, List<JdkBinary>>> binaries;

  JdkRelease(JdkVersion version, boolean psu, Map<BinaryType, List<JdkBinary>> binaries) {
    this.version = version;
    this.psu = psu;

    Map<BinaryType, Map<Arch, List<JdkBinary>>> binMap = new LinkedHashMap<>();
    binaries.entrySet().forEach(e -> {
      e.getValue().forEach(b -> b.setRelease(this));
      binMap.put(e.getKey(), Collections.unmodifiableMap(toMap(e.getValue())));
    });

    this.binaries = Collections.unmodifiableMap(binMap);
  }

  private Map<Arch, List<JdkBinary>> toMap(List<JdkBinary> binaries) {
    Map<Arch, List<JdkBinary>> binMap = new LinkedHashMap<>();
    for (JdkBinary binary : binaries) {
      List<JdkBinary> archBins = binMap.get(binary.getArch());
      if (archBins == null) {
        binMap.put(binary.getArch(), archBins = new ArrayList<>());
      }
      archBins.add(binary);
    }
    return binMap;
  }

  public JdkVersion getVersion() {
    return version;
  }

  public boolean isPsu() {
    return psu;
  }

  public List<JdkBinary> getBinaries(BinaryType type, Arch arch) {
    Map<Arch, List<JdkBinary>> typeBins = binaries.get(type);
    if (typeBins == null) {
      return Collections.emptyList();
    }
    List<JdkBinary> b = typeBins.get(arch);
    return b == null ? Collections.emptyList() : b;
  }

  public Set<Arch> getArchs(BinaryType type) {
    Map<Arch, List<JdkBinary>> typeBins = binaries.get(type);
    if (typeBins == null) {
      return Collections.emptySet();
    }
    return typeBins.keySet();
  }

  public Set<BinaryType> getTypes(Set<BinaryType> allowedTypes) {
    return binaries.keySet().stream()
        .filter(t -> allowedTypes.stream().anyMatch(at -> at.equals(t)))
        .collect(Collectors.toSet());
  }

  public JdkBinary getUnpackableBinary(BinaryType type, Arch arch, String binDescriptor) throws IOException {
    List<JdkBinary> bins = getBinaries(type, arch);
    if (binDescriptor != null) {
      for (JdkBinary bin : bins) {
        if (binDescriptor.equals(bin.getDescriptor())) {
          return bin;
        }
      }
    } else {
      return selectUnpackable(bins);
    }
    return null;
  }

  private static JdkBinary selectUnpackable(List<JdkBinary> binaries) {
    JdkBinary match = null;
    if (binaries != null) {
      int lowest = Integer.MAX_VALUE;
      for (JdkBinary bin : binaries) {
        int idx = UNPACKABLES.indexOf(bin.getDescriptor());
        if (idx != -1 && idx < lowest) {
          lowest = idx;
          match = bin;
        }
      }
    }
    return match;
  }

  private final static List<String> UNPACKABLES = Collections.unmodifiableList(Arrays.asList( //
      "linux-x64.tar.gz", //
      "linux-x64.bin", //
      "linux-amd64.bin", //
      "linux-i586.tar.gz", //
      "linux-i586.bin", //
      "macosx-x64.tar.gz", //
      "macosx-x64.dmg", //
      "osx-x64.tar.gz", //
      "osx-x64.dmg", //
      "windows-x64.zip", //
      "windows-x64.tar.gz", //
      "windows-x64.exe", //
      "windows-x64-p.exe", //
      "windows-amd64.exe", //
      "windows-i586.zip", //
      "windows-i586.tar.gz", //
      "windows-i586.exe", //
      "windows-i586-p.exe", //
      "solaris-sparcv9.tar.gz", //
      "solaris-sparcv9.tar.Z", //
      "solaris-sparcv9.sh", //
      "solaris-x64.tar.gz", //
      "solaris-x64.tar.Z", //
      "solaris-x64.sh", //
      "solaris-amd64.tar.gz", //
      "solaris-amd64.tar.Z", //
      "solaris-amd64.sh" //
  ));
}
