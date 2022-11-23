package net.ddns.crummercraft.servers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateServerFile {

    public static Path path;

    public static void createPath() {
        Path servers = Path.of("servers");
        if (Files.notExists(servers)) {
            try {
                Files.createDirectories(servers);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        path = servers;
    }
}
