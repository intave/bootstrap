package de.jpx3.intave.boot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Versions {
  private final static RemoteResource VERSION_RESOURCE = new RemoteResource("https://service.intave.de/versions", "versions", TimeUnit.DAYS.toMillis(1));
  private final List<Version> content;

  private Versions(List<Version> content) {
    this.content = content;
  }

  public Version lastStable() {
    return findFirstOfStatus(Version.Status.STABLE);
  }

  public Version latest() {
    return findFirstOfStatus(Version.Status.LATEST);
  }

  private Version findFirstOfStatus(Version.Status status) {
    for (Version version : content) {
      if(version.status() == status) {
        return version;
      }
    }
    return null;
  }

  public static Versions lookup() {
    String raw = String.join("", VERSION_RESOURCE.readLines());
    List<Version> versions = parseVersions(raw);
    Collections.sort(versions);
    return new Versions(versions);
  }

  private static List<Version> parseVersions(String rawJson) {
    List<Version> content = new ArrayList<>();
    JsonReader jsonReader = new JsonReader(new StringReader(rawJson));
    jsonReader.setLenient(true);
    JsonArray jsonArray = new JsonParser().parse(jsonReader).getAsJsonArray();
    for (JsonElement jsonElement : jsonArray) {
      content.add(Version.parseFrom(jsonElement));
    }
    return content;
  }
}
