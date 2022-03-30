package de.jpx3.intave.boot;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public final class RemoteResource {
  private final String origin;
  private final String name;
  private final long expiration;

  public RemoteResource(String origin, String name, long expiration) {
    this.origin = origin;
    this.name = name;
    this.expiration = expiration;
  }

  public List<String> readLines() {
    InputStream inputStream;
    try {
      inputStream = read();
    } catch (IllegalStateException exception) {
      refreshFile();
      inputStream = read();
    }
    Scanner scanner = new Scanner(inputStream, "UTF-8");
    List<String> lines = new ArrayList<>();
    while (scanner.hasNext()) {
      lines.add(scanner.next());
    }
    try {
      inputStream.close();
    } catch (IOException ignored) {}
    return lines;
  }

  public InputStream read() {
    prepareFile();
    try {
      return new FileInputStream(fileStore());
    } catch (Exception | Error e) {
      fileStore().delete();
      return new ByteArrayInputStream(new byte[0]);
    }
  }

  private boolean prepareFile() {
    File file = fileStore();
    long fileLastModified = System.currentTimeMillis() - file.lastModified();
    boolean invalidFile = !file.exists() || fileLastModified > expiration;

    if(invalidFile) {
      refreshFile();
    }
    return file.exists();
  }

  private boolean refreshFile() {
    File file = fileStore();
    if(file.exists()) {
      file.delete();
    }
    try {
      file.getParentFile().mkdirs();
      file.createNewFile();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
    // try download
    try {
      URL remoteFileAddress = new URL(origin);
      URLConnection urlConnection = remoteFileAddress.openConnection();
      urlConnection.addRequestProperty("User-Agent", "IntaveBootstrap");
      urlConnection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
      urlConnection.addRequestProperty("Pragma", "no-cache");
      urlConnection.setConnectTimeout(3000);
      urlConnection.setReadTimeout(3000);
      InputStream inputStream = urlConnection.getInputStream();
      FileOutputStream outputStream = new FileOutputStream(fileStore());
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, length);
      }
      file.setLastModified(System.currentTimeMillis());
      inputStream.close();
      outputStream.close();
    } catch (Exception exception) {
      exception.printStackTrace();
      return false;
    }
    return file.exists();
  }

  private File fileStore() {
    String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    File workDirectory;
    String filePath;
    if(operatingSystem.contains("win")) {
      filePath = System.getenv("APPDATA") + "/Intave/Bootstrap/";
    } else {
      filePath = System.getProperty("user.home") + "/.intave/bootstrap/";
    }
    workDirectory = new File(filePath);
    if(!workDirectory.exists()) {
      workDirectory.mkdir();
    }
    return new File(workDirectory, name);
  }
}
