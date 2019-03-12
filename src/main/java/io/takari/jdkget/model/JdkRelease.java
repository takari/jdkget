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
    return binaries.keySet().stream().filter(t -> allowedTypes.stream().anyMatch(at -> at.equals(t)))
        .collect(Collectors.toSet());
  }

  public JdkBinary getUnpackableBinary(BinaryType type, Arch arch) throws IOException {
    List<JdkBinary> bins = getBinaries(type, arch);
    JdkBinary match = null;
    if (bins != null) {
      int lowest = Integer.MAX_VALUE;
      outer: for (JdkBinary bin : bins) {
        String fname = filename(bin.getPath()).toLowerCase();

        for (String skip : NON_UNPACKABLES) {
          if (fname.endsWith(skip)) {
            continue outer;
          }
        }

        int idx = 0;
        for (String ext : UNPACKABLES) {
          if (fname.endsWith(ext)) {
            break;
          }
          idx++;
        }

        if (idx < UNPACKABLES.size() && idx < lowest) {
          lowest = idx;
          match = bin;
        }
      }
    }
    return match;
  }

  private static String filename(String path) {
    int sl = path.lastIndexOf('/');
    return sl == -1 ? path : path.substring(sl + 1);
  }

  private final static List<String> UNPACKABLES = Collections.unmodifiableList(Arrays.asList( //
      ".tar.gz", ".tar.z", ".zip", ".bin", ".sh", ".dmg", ".exe"));

  private final static List<String> NON_UNPACKABLES = Collections.unmodifiableList(Arrays.asList( //
      "-rpm.bin"));
}
