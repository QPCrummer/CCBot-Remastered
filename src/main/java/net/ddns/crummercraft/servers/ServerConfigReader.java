package net.ddns.crummercraft.servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
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
    private static List<File> listServerFiles() {
        try (Stream<Path> stream = Files.walk(createPathIfMissing())) {
            return stream.map(Path::toFile).filter(File::isFile).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<ServerData> listServerInfos() {
        final ObjectReader parser = new ObjectMapper().readerFor(ServerData.class);
        return listServerFiles().stream().map(file -> {
            try {
                return (ServerData) parser.readValue(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public static List<Server> listServers(Consumer<MessageReceivedEvent> onMainStart, Consumer<MessageReceivedEvent> onMainStop, Consumer<MessageReceivedEvent> onProxyStart, Consumer<MessageReceivedEvent> onProxyStop) {
        return listServerInfos().stream().map(serverInfo -> {
            if (serverInfo.isMainServer()) {
                return new Server(serverInfo, onMainStart, onMainStop);
            } else if (serverInfo.isProxy()) {
                return new Proxy(serverInfo, onProxyStart, onProxyStop);
            } else {
                return new Server(serverInfo, NOOP, NOOP);
            }
        }).toList();
    }
}