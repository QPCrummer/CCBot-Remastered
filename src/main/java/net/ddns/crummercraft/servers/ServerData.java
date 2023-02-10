package net.ddns.crummercraft.servers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerData(String name,
                         String minecraftVersion,
                         String serverFolder,
                         int serverPort,
                         String serverIP,
                         boolean isMainServer,
                         boolean isProxy) {

    @JsonCreator
    public ServerData(@JsonProperty("name") String name,
                      @JsonProperty("version") String minecraftVersion,
                      @JsonProperty("path") String serverFolder,
                      @JsonProperty("port") int serverPort,
                      @JsonProperty("ip") String serverIP,
                      @JsonProperty("isMainServer") boolean isMainServer,
                      @JsonProperty("isProxy") boolean isProxy) {

        this.name = name;
        this.minecraftVersion = minecraftVersion;
        this.serverFolder = serverFolder;
        this.serverPort = serverPort;
        this.serverIP = serverIP;
        this.isMainServer = isMainServer;
        this.isProxy = isProxy;
    }
}
