package net.ddns.crummercraft.servers;

import net.ddns.crummercraft.Proxy;
import net.ddns.crummercraft.Server;
import net.dv8tion.jda.api.OnlineStatus;

import java.io.File;
import java.util.ArrayList;

import static net.ddns.crummercraft.servers.Json2Server.serverJsonArray;


public class Hashmap2ArrayList {

    public static ArrayList<Server> serverArrayList = new ArrayList<>();
    public static Server mainServer;
    public static Proxy proxyServer;

    public static void fillArrayList() {

        Server[] server = new Server[serverJsonArray.size()];

        for (int i = 0; i < serverJsonArray.size(); i++) {
            ServerJson serverJson = serverJsonArray.get(i);
            server[i] = new Server(new File(serverJson.getPath() + "/start.sh"), serverJson.getName(), serverJson.getIp() + ":" + serverJson.getPort());
            serverArrayList.add(server[i]);
        }
    }

    public static void findMainServer() {

        for (ServerJson serverJson : serverJsonArray) {
            if (serverJson.isMainServer()) {
                mainServer = new Server(new File(serverJson.getPath() + "/start.sh"), serverJson.getName(), serverJson.getIp() + ":" + serverJson.getPort(), e -> {
                    if (proxyServer.isRunning()) {
                        e.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                    } else {
                        e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);
                    }
                }, e -> {
                    if (proxyServer.isRunning()) {
                        e.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
                    } else {
                        e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);
                    }
                });
            }
        }
    }

    public static void findProxy() {

        for (ServerJson serverJson : serverJsonArray) {
            if (serverJson.isProxy()) {
                proxyServer = new Proxy(new File(serverJson.getPath() + "/start.sh"), serverJson.getName(), serverJson.getIp() + ":" + serverJson.getPort(), e -> {
                    if (mainServer.isRunning()) {
                        e.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                    } else {
                        e.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
                    }
                }, e -> e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE));
            }
        }

    }
}
