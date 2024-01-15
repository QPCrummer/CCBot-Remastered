package net.ddns.crummercraft.servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import net.ddns.crummercraft.Main;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerConfigReader {

    private static final Consumer<MessageReceivedEvent> NOOP = e -> {
    };

    private static Path createPathIfMissing() {
        final Path servers = Path.of("servers");
        if (Files.notExists(servers)) {
            try {
                Files.createDirectories(servers);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return servers;
    }

    // To whomever sees this monstrosity: I am sorry that this was invented
    public static Map<String, Server> listServers(
            Consumer<MessageReceivedEvent> onMainStart,
            Consumer<MessageReceivedEvent> onMainStop,
            Consumer<MessageReceivedEvent> onProxyStart,
            Consumer<MessageReceivedEvent> onProxyStop) {
        final ObjectReader parser = new ObjectMapper().readerFor(ServerData.class);
        try (Stream<Path> stream = Files.walk(createPathIfMissing())) {
            return stream.map(Path::toFile)
                    .filter(File::isFile)
                    .map(file -> {
                        try {
                            return (ServerData)parser.readValue(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(serverData -> serverData != null && serverData.name() != null && checkIfServerExists(serverData))
                    .collect(Collectors.toMap(ServerData::name, serverInfo -> createServer(serverInfo, onMainStart, onMainStop, onProxyStart, onProxyStop)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Server createServer(ServerData serverInfo, Consumer<MessageReceivedEvent> onMainStart, Consumer<MessageReceivedEvent> onMainStop, Consumer<MessageReceivedEvent> onProxyStart, Consumer<MessageReceivedEvent> onProxyStop) {
        if (serverInfo.isMainServer()) {
            return new Server(serverInfo, onMainStart, onMainStop);
        } else if (serverInfo.isProxy()) {
            return new Proxy(serverInfo, onProxyStart, onProxyStop);
        } else {
            return new Server(serverInfo, NOOP, NOOP);
        }
    }

    private static boolean checkIfServerExists(ServerData data) {
        Path path = Paths.get(data.serverFolder());
        if (Files.notExists(path)) {
            Main.LOGGER.warn(data.name() + " does not exist: " + path + "! Ignoring!");
            return false;
        } else {
            return true;
        }
    }
}