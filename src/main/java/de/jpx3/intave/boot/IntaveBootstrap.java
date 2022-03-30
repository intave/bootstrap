package de.jpx3.intave.boot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

public final class IntaveBootstrap extends JavaPlugin {
  private final static String PREFIX = "[Intave/Boot]";
  private final Resource versionInfo = new Resource("version");
  private final Resource intaveResource = new Resource("intave");

  private Plugin intavePlugin;

  @Override
  public void onEnable() {
    if  (getServer().getPluginManager().getPlugin("Intave") != null) {
      throw new IllegalStateException("You already have Intave on your server! IntaveBootstrap is a LOADER not an external UPDATER, please remove the Intave jar from your plugin directory for IntaveBootstrap to work properly");
    }
    log("When you run into any issues with this updater, try re-downloading it");
    Version requiredVersion = requiredVersion();
    if (updateNeeded(requiredVersion)) {
      log("Updating..");
      performUpdateTo(requiredVersion);
    }
    boot();
  }

  private Version requiredVersion() {
    Versions versions = Versions.lookup();
    return versionSelect().selectFrom(versions);
  }

  private VersionSelect versionSelect() {
    prepareConfigFile();
    return VersionSelect.findOrDie(getConfig().getString("version", "STABLE"));
  }

  private boolean updateNeeded(Version requestedVersion) {
    if (versionInfo.exists()) {
      Scanner scanner = new Scanner(versionInfo.read());
      String myVersion = scanner.nextLine().trim();
      log(requestedVersion.name() + " requested, current is " + myVersion);
      return !intaveResource.exists() || !myVersion.equalsIgnoreCase(requestedVersion.name());
    } else {
      return !intaveResource.exists();
    }
  }

  private void performUpdateTo(Version lastStable) {
    String authKey = authkey();
    if (authKey == null || authKey.length() != 128) {
      throw new IllegalStateException("Please enter a valid authkey");
    }
    String targetVersion = lastStable.name();
    log("Updating to " + targetVersion);
    versionInfo.write(new ByteArrayInputStream(targetVersion.getBytes(StandardCharsets.UTF_8)));
    CookieManager sessionDetails = authenticate(authKey);
    downloadIntaveJar(sessionDetails, targetVersion);
  }

  private void boot() {
    log("Booting..");
    loadJar();
    if (intavePlugin == null || !intavePlugin.isEnabled()) {
      log("Boot failed");
    }
  }

  private void log(String string) {
    Bukkit.getLogger().log(Level.INFO, PREFIX + " " + string);
  }

  private String authkey() {
    File configFile = prepareConfigFile();
    try {
      FileInputStream fileInputStream = new FileInputStream(configFile);
      YamlConfiguration configuration = new YamlConfiguration();
      InputStreamReader reader = new InputStreamReader(fileInputStream);
      configuration.load(reader);
      fileInputStream.close();
      reader.close();
      return configuration.getString("authkey");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    return null;
  }

  private File prepareConfigFile() {
    File dataFolder = getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdir()) {
      throw new IllegalStateException("Failed to create data folder");
    }
    File configFile = new File(dataFolder, "config.yml");
    if (!configFile.exists()) {
      saveResource("config.yml", false);
    }
    return configFile;
  }

  @Override
  public void onDisable() {
    if (intavePlugin != null) {
      getServer().getPluginManager().disablePlugin(intavePlugin);
      intavePlugin = null;
    }
  }

  private CookieManager authenticate(String bootstrapKey) {
    try {
      URL remoteFileAddress = new URL("https://intave.de/keyauthenticate.php");
      URLConnection connection = remoteFileAddress.openConnection();
      connection.addRequestProperty("User-Agent", "IntaveBootstrap");
      connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
      connection.addRequestProperty("Pragma", "no-cache");
      connection.addRequestProperty("Key", bootstrapKey);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      InputStream inputStream = connection.getInputStream();
      Scanner scanner = new Scanner(inputStream);
      StringBuilder outputBuilder = new StringBuilder();
      while (scanner.hasNextLine()) {
        outputBuilder.append(scanner.nextLine());
      }
      String output = outputBuilder.toString();
      if (!output.equalsIgnoreCase("success")) {
        intaveResource.write(new ByteArrayInputStream(new byte[0]));
        throw new IllegalStateException("Invalid response: " + output);
      }
      CookieManager cookieManager = new CookieManager();
      Map<String, List<String>> headerFields = connection.getHeaderFields();
      List<String> cookies = headerFields.get("Set-Cookie");
      if (cookies == null || cookies.isEmpty()) {
        cookies = headerFields.get("set-cookie");
      }
      if (cookies != null) {
        cookies.forEach(cookie -> cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0)));
      }
      if (cookies == null || cookies.isEmpty()) {
        headerFields.forEach((s, strings) -> System.out.println(s + " -> " + strings));
        throw new IllegalStateException("Failed to retrieve cookies: " + output);
      }
      return cookieManager;
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private void downloadIntaveJar(CookieManager cookieManager, String version) {
    try {
      URL remoteFileAddress = new URL("https://intave.de/download-intave.php");
      URLConnection connection = remoteFileAddress.openConnection();
      connection.addRequestProperty("User-Agent", "IntaveBootstrap");
      connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
      connection.addRequestProperty("Pragma", "no-cache");
      connection.addRequestProperty("RequestedVersion", version);
      String join = join(cookieManager.getCookieStore().getCookies());
      connection.addRequestProperty("Cookie", join);
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(30000);
      intaveResource.write(connection.getInputStream());
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private String join(Collection<?> objects) {
    if (objects.isEmpty()) {
      throw new IllegalStateException("No cookies found :( " + objects);
    }
    StringBuilder stringBuilder = new StringBuilder();
    objects.forEach(object -> stringBuilder.append(object).append(";"));
    return stringBuilder.substring(0, stringBuilder.length() - 1);
  }

  private void loadJar() {
    try {
      File originalFile = intaveResource.fileStore();
      if (!runtimeJarDatafolder().mkdirs()) {
        throw new IllegalStateException("Failed to create runtime folder");
      }
      Files.setAttribute(runtimeJarDatafolder().toPath(), "dos:hidden", true);
      File targetFile = new File(runtimeJarDatafolder(), "intave.jar");
      copy(originalFile, targetFile);
      intavePlugin = getServer().getPluginManager().loadPlugin(targetFile);
      intavePlugin.onLoad();
      getServer().getPluginManager().enablePlugin(intavePlugin);
    } catch (InvalidPluginException | IOException | InvalidDescriptionException e) {
      e.printStackTrace();
    }
  }

  private File runtimeJarDatafolder() {
    return new File(getDataFolder(), "/.runtime/");
  }

  private void copy(File source, File dest) throws IOException {
    try (InputStream is = new FileInputStream(source);
         OutputStream os = new FileOutputStream(dest)) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = is.read(buffer)) != -1) {
        os.write(buffer, 0, length);
      }
    }
  }
}
