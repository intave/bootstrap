package de.jpx3.intave.boot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

public final class Version implements Comparable<Version> {
  private final String name;
  private final long release;
  private final Status typeClassifier;

  public Version(String name, long release, Status typeClassifier) {
    this.name = name;
    this.release = release;
    this.typeClassifier = typeClassifier;
  }

  public String name() {
    return name;
  }

  public long release() {
    return release;
  }

  public Status status() {
    return typeClassifier;
  }

  @Override
  public int compareTo(Version other) {
    return Long.compare(other.release, release);
  }

  public static Version parseFrom(JsonElement jsonElement) {
    JsonObject jsonObject = jsonElement.getAsJsonObject();
    String name = jsonObject.get("name").getAsString();
    String release = jsonObject.get("release").getAsString();
    String status = jsonObject.get("status").getAsString();

    return new Version(
      name, Long.parseLong(release),
      Status.valueOf(status.toUpperCase(Locale.ROOT))
    );
  }

  public enum Status {
    OUTDATED("OUTDATED"),
    LATEST("LATEST"),
    STABLE("STABLE"),
    INVALID("")

    ;

    String name;

    Status(String name) {
      this.name = name;
    }

    public String className() {
      return name;
    }
  }
}
