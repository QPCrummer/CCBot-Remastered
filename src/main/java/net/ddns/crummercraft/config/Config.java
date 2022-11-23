package net.ddns.crummercraft.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class Config {
    public static String cfgver;

    public static Properties properties = new Properties();

    public static String path;
    public static String token;
    public static String admin_role;
    public static String owner_role;
    public static String private_channel;
    public static String website;

    static {
        path = Paths.get("") + "ccbot.properties";
    }

    public Config() {
        if (Files.notExists(Path.of(path))) {
            mkfile();
        } else {
            loadcfg();
            cfgver = properties.getProperty("config-version");
            if (!(Objects.equals(cfgver, "1.0"))) {
                mkfile();
            } else {
                parse();
            }
        }
    }

    public void mkfile() {
        try (OutputStream output = new FileOutputStream(path)) {
            if (!properties.contains("config-version")) {
                properties.setProperty("config-version", "1.0");
            }
            if (!properties.contains("token")) {
                properties.setProperty("token", "insert token");
            }
            if (!properties.contains("admin")) {
                properties.setProperty("admin", "Admin");
            }
            if (!properties.contains("owner")) {
                properties.setProperty("owner", "Owner");
            }
            if (!properties.contains("private_channel")) {
                properties.setProperty("private_channel", "#private");
            }
            if (!properties.contains("website")) {
                properties.setProperty("website", "This Discord's Website: www.example.com");
            }
            properties.store(output, null);
            parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadcfg() {
        try (InputStream input = new FileInputStream(path)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parse() {
        cfgver = properties.getProperty("config-version");
        token = properties.getProperty("token");
        admin_role = properties.getProperty("admin");
        owner_role = properties.getProperty("owner");
        private_channel = properties.getProperty("private_channel");
        website = properties.getProperty("website");
    }
}