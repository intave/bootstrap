package de.jpx3.intave.boot;

import java.util.Arrays;

/**
 * Class generated using IntelliJ IDEA
 * Created by Richard Strunk 2021
 */

public enum VersionSelect {
  LATEST(Versions::latest),
  STABLE(Versions::lastStable);

  private final VersionSelector selector;

  VersionSelect(VersionSelector selector) {
    this.selector = selector;
  }

  public Version selectFrom(Versions versions) {
    return selector.selectFrom(versions);
  }

  public static VersionSelect findOrDie(String classif) {
    return Arrays.stream(values())
      .filter(versionSelect -> versionSelect.name().equalsIgnoreCase(classif)).findFirst()
      .orElseThrow(() -> new IllegalStateException("Unable to locate version select \"" + classif + "\""));
  }
}
