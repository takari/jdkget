package io.takari.jdkget.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum BinaryType {
  JDK("jdk"), JRE("jre"), SERVERJRE("serverjre");

  private String name;

  BinaryType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static BinaryType getDefault() {
    return JDK;
  }

  public static Set<BinaryType> forNames(String[] typeNames) {
    return forNames(typeNames == null ? null : Arrays.asList(typeNames),
        Collections.singleton(BinaryType.getDefault()));
  }

  public static Set<BinaryType> forNames(List<String> typeNames) {
    return forNames(typeNames, Collections.singleton(BinaryType.getDefault()));
  }

  public static Set<BinaryType> forNames(List<String> typeNames, Set<BinaryType> defaultTypes) {
    if (typeNames == null) {
      return defaultTypes;
    }

    Set<BinaryType> types = EnumSet.noneOf(BinaryType.class);
    for (String typeName : typeNames) {
      BinaryType type = forName(typeName, null);
      if (type != null) {
        types.add(type);
      }
    }

    return types.isEmpty() ? defaultTypes : types;
  }

  public static BinaryType forName(String typeName) {
    return forName(typeName, getDefault());
  }

  public static BinaryType forName(String typeName, BinaryType defaultType) {
    for (BinaryType t : values()) {
      if (t.getName().equals(typeName)) {
        return t;
      }
    }
    return defaultType;
  }
}