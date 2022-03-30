package de.jpx3.intave.boot;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

public final class Resource {
  private final String name;

  public Resource(String name) {
    this.name = name;
  }

  public InputStream read() {
    if (!fileStore().exists()) {
      throw new IllegalStateException();
    }
    fileStore().setLastModified(System.currentTimeMillis());
    try {
      return new FileInputStream(fileStore());
    } catch (Exception | Error e) {
      throw new IllegalStateException("Unable to access resource file");
    }
  }

  public boolean write(InputStream inputStream) {
    File file = fileStore();
    if (file.exists()) {
      file.delete();
    }
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    try {
      ReadableByteChannel byteChannel = Channels.newChannel(inputStream);
      FileOutputStream fileOutputStream = new FileOutputStream(fileStore());
      fileOutputStream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
      file.setLastModified(System.currentTimeMillis());
      fileOutputStream.close();
    } catch (Exception exception) {
      exception.printStackTrace();
      return false;
    }
    return file.exists();
  }

  public boolean exists() {
    File file = fileStore();
    return file.exists();
  }

  public File fileStore() {
    String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    File workDirectory;
    String filePath;
    if (operatingSystem.contains("win")) {
      filePath = System.getenv("APPDATA") + "/Intave/Bootstrap/";
    } else {
      filePath = System.getProperty("user.home") + "/.intave/bootstrap/";
    }
    workDirectory = new File(filePath);
    if (!workDirectory.exists()) if (!workDirectory.mkdirs()) {
      throw new IllegalStateException("Unable to create directory " + workDirectory.getAbsolutePath());
    }
    return new File(workDirectory, name + ".jx");
  }
}
