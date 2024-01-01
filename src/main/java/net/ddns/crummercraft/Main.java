package net.ddns.crummercraft;

import net.ddns.crummercraft.config.Config;
import net.ddns.crummercraft.servers.Server;
import net.ddns.crummercraft.servers.ServerConfigReader;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;
import static net.ddns.crummercraft.ChatUtils.findUUID;
import static net.ddns.crummercraft.config.Config.*;

public class Main extends ListenerAdapter {
    public static final Logger LOGGER = LoggerFactory.getLogger("CCBot");
    private final List<Server> servers;
    private final File ipList;
    public String serverList;

    private Server mainServer = null;
    private Server proxyServer = null;

    public Main() {
        this.servers = ServerConfigReader.listServers(e -> {
            if (proxyServer != null) {
                if (proxyServer.isRunning()) {
                    e.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                } else {
                    e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);
                }
            }
        }, e -> {
            if (proxyServer != null) {
                if (proxyServer.isRunning()) {
                    e.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
                } else {
                    e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);
                }
            }
        }, e -> {
            if (mainServer.isRunning()) {
                e.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
            } else {
                e.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
            }
        }, e -> e.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE));

        mainServer = servers.stream().filter(server -> server.info.isMainServer()).findFirst().orElseThrow(() -> new RuntimeException("No main server found"));
        proxyServer = servers.stream().filter(server -> server.info.isProxy()).findFirst().orElse(null);

        this.ipList = new File(new File(mainServer.info.serverFolder(), "config"), "offline-ip-list.txt");

        this.serverList = servers.stream().sorted(comparingInt(Server::port)).map(Server::toString).collect(joining("\n"));
    }

    public static void main(String[] args) {
        new Config();
        JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.OFFLINE)
                .addEventListeners(new Main())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
    }

    public static void answer(MessageReceivedEvent e, String message) {
        if (e.getMember() == null) {
            LOGGER.warn(e.getAuthor().getName() + " didn't receive this message:" + message);
        }
        e.getChannel().sendMessage(message).submit();
    }

    @Override
    public synchronized void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) {
            return;
        }
        if (e.getMember() == null) {
            if (e.getMessage().getContentDisplay().startsWith("!changeIP ")) {
                final String[] s = e.getMessage().getContentDisplay().substring(4).split(" ");
                changeIP(e, s[1], s[2]);
            } else {
                e.getChannel().sendMessage("Use !changeIP <4 digit code> <newip> to update your ip address.").submit();
            }
            return;
        }
        final String message = e.getMessage().getContentDisplay().replaceAll("\\s.*", "");
        switch (message) {

            case "!help" -> answer(e, """ 
                    ```yaml
                    Welcome to CCBot V3.1.0, How may I assist you?
                    !ip - #Get the server status and player count
                    !status - #Get status for each server
                    !servers - #Lists all servers
                    !myip - #Find what your current IP is
                    !uuid - #Find out how to obtain your UUID
                    !web - #Gives the link to the CC website
                    !mods - #Instructions for installing FabricMC
                    !CC help - #List admin features");
                    ```""");
            case "!status" -> status(e);
            case "!ip" -> answer(e, "```yaml\nServer IPs:\n" + serverList + "```");
            case "!myip" -> answer(e, "You can get your ip using: https://api64.ipify.org/");
            case "!servers" -> answer(e, "```yaml\nServers:\n" + serverList + "```");
            case "!web" -> answer(e, website);
            case "!uuid" -> answer(e, findUUID(e));
            case "!mods" -> answer(e, """
                    ```yaml
                    Here is a tutorial for installing mods for Fabric: https://sites.google.com/view/crummer-craft/how-to-install-mods?authuser=1
                    ```""");
        }
        if (message.startsWith("!CC")) {
            if (e.getMember().getRoles().stream().noneMatch(role -> role.getName().equals(admin_role))) {
                answer(e, "You must be an admin to use these commands!");
            } else {
                try {
                    handleAdmin(e);
                } catch (InterruptedException ex) {
                    LOGGER.error("Failed to use admin commands", ex);
                }
            }
        }
    }

    private Stream<Server> findServer(MessageReceivedEvent e, String name) {
        if (name == null) {
            return servers.stream();
        }
        return Stream.ofNullable(servers.stream().filter(server -> server.name().equals(name)).findFirst().orElseGet(() -> {
            answer(e, "The server named " + name + " was not found");
            return null;
        }));
    }

    private void status(MessageReceivedEvent e) {
        answer(e, """
                          ```yaml
                          Servers:
                          """ + servers.stream().map(Server::status).collect(joining("\n")) + "```");
    }

    private void handleAdmin(MessageReceivedEvent e) throws InterruptedException {
        final String[] s = e.getMessage().getContentDisplay().substring(4).split(" ");
        final String action = s[0];
        final String name = s.length == 1 ? null : s[1];
// CCBot's main functions

        switch (action.toLowerCase()) {
            case "start" -> answer(e, findServer(e, name).map(server -> server.start(e)).collect(joining("\n")));
            case "stop" -> answer(e, findServer(e, name).map(Server::stop).collect(joining("\n")));
            case "restart" -> answer(e, findServer(e, name).map(server -> server.restart(e)).collect(joining("\n")));
            case "kill" -> findServer(e, name).forEach(server -> server.kill(e));
            case "exec" -> findServer(e, name).forEach(server -> server.exec(e, Arrays.stream(s).skip(2).collect(joining(" "))));
            case "override" -> findServer(e, name).forEach(server -> server.override(e));
            case "external_stop" -> externalStop(e, name);
            case "external_kill" -> externalKill(e, name);
            case "jar_list" -> listRunningJars(e);
            case "pid" -> findServer(e, name).forEach(server -> server.pid(e));
// Logs
            case "clear_logs" -> findServer(e, name).forEach(server -> server.clearLogs(e));
            case "logs" -> findServer(e, name).forEach(server -> server.readLatestLog(e));
// IP
            case "iplist" -> readIps(e);
            case "setip" -> writeIps(e, Arrays.stream(s).skip(1).collect(joining(" ")));
            case "addplayer" -> addPlayer(e, s[1], s[2], s[3]);
            case "removeplayer" -> removePlayer(e, s[1]);
            case "changeip" -> changeIP(e, s[1], s[2]);
// Bot
            case "bot_terminate" -> answer(e, "Are you sure you want to do this? This will kill the bot permanently! Run !CC term_continue to proceed.");
            case "term_continue" -> System.exit(0);
            case "bot_pause" -> Thread.sleep(3600000);
// Basic Help command that list these actions^
            case "help" -> answer(e, """
                    ```yaml
                    Welcome to CCBot 3.2.0, An Open-Source Server Management Companion!
                    I offer many helpful options for your convenience.
                                        
                    Do !CC [category]
                    Help categories:
                    - help-server
                    #info about server commands
                                        
                    - help-log
                    #info about logging
                                        
                    - help-offline
                    #info about offline players

                    - help-bot
                    #info about bot management
                                     
                    The [server] argument allows specifying servers.
                    Do this by adding the server's name at the end of a command.
                    If it is not added, the cmd will be applied to all servers.
                    ```""");
            case "help-server" -> answer(e, """
                    ```yaml
                    Try !CC with:
                    - start [server] #starts servers
                    - stop [server] #stops servers
                    - restart [server] #restarts servers
                    - kill [server] #kills servers; only do if needed
                    - exec <server> #executes commands in-game
                    - override [server] #override other active commands
                    - external_stop <pid> #send a stop signal to a java process
                    - external_kill <pid> #kill a java process
                    - jar_list #lists all running java processes
                    - pid [server] #shows the server's process id
                    ```""");
            case "help-log" -> answer(e, """
                    ```yaml
                    Try !CC with:
                    - clear_logs [server] #clears the saved logs
                    - logs [server] #gives the latest.log file
                    ```""");
            case "help-offline" -> answer(e, """
                    ```yaml
                    Try !CC with:
                    - iplist #Only Co-Overlord can do that, read ip list
                    - setip [ips] #Only Co-Overlord can do that, write the whole ip list
                    - addplayer [code] [uuid] [name]#add ip
                    - removeplayer [code] #Removes a player
                    - changeip [code] [newip] #update the ip
                    You can do most of these commands in the DMs with the bot.
                    ```""");
            case "help-bot" -> answer(e, """
                    ```yaml
                    Try !CC with:
                    - bot_pause #stops the bot for 1 hour
                    - bot_terminate #kills the bot
                    If the bot needs to be killed for whatever reason, run !CC bot_terminate.
                    THIS ACTION CANNOT BE UNDONE!
                    ```""");

            default -> answer(e, "That isn't one of my options. Try !CC help for my options");
        }
    }

    private byte[] readIpList(MessageReceivedEvent e) {
        try (InputStream stream = new FileInputStream(ipList)) {
            return stream.readAllBytes();
        } catch (IOException ioException) {
            LOGGER.error("An IO error occurred while reading", ioException);
            answer(e, "An IO error occurred while reading!");
            return null;
        }
    }

    private void writeIpList(MessageReceivedEvent e, byte[] value) {
        try (OutputStream stream = new FileOutputStream(ipList)) {
            stream.write(value);
            stream.flush();
        } catch (IOException ioException) {
            LOGGER.error("An IO error occurred while reading", ioException);
            answer(e, "An IO error occurred while writing!");
            return;
        }
        answer(e, "File updated");
    }

    private void removePlayer(MessageReceivedEvent e, String code) {
        if (notOwner(e)) {
            return;
        }
        final byte[] bytes = readIpList(e);
        if (bytes == null) {
            return;
        }
        final byte[] newValue = Arrays.stream(new String(bytes).split("\n"))
                .filter(s -> !s.startsWith(code + "|"))
                .collect(joining("\n")).getBytes();

        writeIpList(e, newValue);
    }

    // Add an IP to online-ip-list text file
    private void addPlayer(MessageReceivedEvent e, String code, String uuid, String name) {
        if (notOwner(e)) {
            return;
        }
        if (code.contains("|")) {
            answer(e, "Message should not contain any `|`");
            return;
        }

        final byte[] bytes = readIpList(e);
        if (bytes == null) {
            return;
        }
        final byte[] newValue = (new String(bytes) + "\n" + code + "|" + "0.0.0.0" + "|" + uuid + "|" + name).getBytes();
        writeIpList(e, newValue);
    }

    private void changeIP(MessageReceivedEvent e, String code, String newip) {
        LOGGER.info(e.getMessage().getContentDisplay());
        if (newip.contains("|") || newip.contains("\n") || newip.contains("\0")) {
            answer(e, "Parameters should not contain any special characters");
            return;
        }
        final byte[] bytes = readIpList(e);
        if (bytes == null) {
            return;
        }
        final byte[] newValue = Arrays.stream(new String(bytes).split("\n"))
                .map(s -> {
                    final String[] split = s.split("\\|");
                    return (!s.startsWith(code + "|")) ? s : (code + "|" + newip + "|" + split[2] + "|" + split[3]);
                })
                .collect(joining("\n")).getBytes();
        writeIpList(e, newValue);
    }

    private boolean notOwner(MessageReceivedEvent e) {
        final Member member = e.getMember();
        assert member != null;
        if (member.getRoles().stream().anyMatch(role -> role.getName().equals(owner_role))) {
            if (!private_channel.equals(e.getChannel().getName())) {
                answer(e, "You must execute this command in " + private_channel + ".");
                return true;
            }
            return false;
        }
        answer(e, "Sorry, you aren't the owner. Please contact the owner for help.");
        return true;
    }


    // Writing IPs to offline-ip-list text file
    private void writeIps(MessageReceivedEvent e, String message) {
        if (notOwner(e)) {
            return;
        }
        try (OutputStream stream = new FileOutputStream(ipList)) {
            stream.write(message.getBytes());
            stream.flush();
        } catch (IOException ioException) {
            LOGGER.error("An IO error occurred while reading", ioException);
            answer(e, "An IO error occurred!");
            return;
        }
        answer(e, "File updated");
    }

    private void readIps(MessageReceivedEvent e) {
        if (notOwner(e)) {
            return;
        }
        e.getChannel().sendFiles(FileUpload.fromData(ipList)).submit();
    }

    // PID lister
    private void externalKill(MessageReceivedEvent e, String pid) {
        if (jarContains(pid)) {
            answer(e, "The process " + pid + " is going to be destroyed");
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/kill", "-9", pid});
            } catch (IOException ioException) {
                LOGGER.error("Couldn't destroy the server", ioException);
                answer(e, "Couldn't destroy the server!");
            }
        } else {
            answer(e, "Pid not found!");
            listRunningJars(e);
        }
        answer(e, "All running jars:");
        listRunningJars(e);
    }

    private void externalStop(MessageReceivedEvent e, String pid) {
        if (jarContains(pid)) {
            answer(e, "The process " + pid + " is going to be stopped");
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/kill", pid});
            } catch (IOException ioException) {
                LOGGER.error("Couldn't stop the server", ioException);
                answer(e, "Couldn't stop the server!");
            }
        } else {
            answer(e, "Pid not found!");
            listRunningJars(e);
        }
        answer(e, "Here's the list of running jars:");
        listRunningJars(e);
    }

    private void listRunningJars(MessageReceivedEvent e) {
        try {
            final StringBuilder builder = new StringBuilder();
            final Process process = Runtime.getRuntime().exec(new String[]{"/bin/ps", "-eo", "pid,comm,command"});
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("java")) {
                    builder.append(line).append('\n');
                }
            }
            reader.close();
            answer(e, builder.toString());
            process.destroy();
        } catch (IOException ioException) {
            LOGGER.error("Couldn't destroy the processes", ioException);
        }
    }

    private boolean jarContains(String pid) {
        try {
            final Process process = Runtime.getRuntime().exec(new String[]{"/bin/ps", "-eo", "pid,comm"});
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("java")) {
                    final String jarPid = line.substring(0, line.indexOf('j') - 1).trim();
                    if (pid.equals(jarPid)) {
                        reader.close();
                        process.destroy();
                        return true;
                    }
                }
            }
            reader.close();
            process.destroy();
        } catch (IOException ioException) {
            LOGGER.error("Couldn't destroy the process", ioException);
        }
        return false;
    }
}
