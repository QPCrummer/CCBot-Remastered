package net.ddns.crummercraft;

import net.ddns.crummercraft.config.Config;
import net.ddns.crummercraft.servers.ServerJson;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.ddns.crummercraft.config.Config.*;
import static net.ddns.crummercraft.servers.CreateServerFile.createPath;
import static net.ddns.crummercraft.servers.Hashmap2ArrayList.*;
import static net.ddns.crummercraft.servers.Json2Server.jsonRun;
import static net.ddns.crummercraft.servers.Json2Server.serverJsonArray;
import static net.ddns.crummercraft.utils.ChatUtils.findUUID;

public class Main extends ListenerAdapter {

    private final Server crummerCraft;
    private final List<Server> servers;
    private final File ipList;
    public String list_ips;
    public String list_servers;
    // List of servers

    public Main() {
        this.crummerCraft = mainServer;
        if (this.crummerCraft == null) {
            System.out.println("Please set a main server in server.json!");
            System.exit(0);
        }

        this.servers = serverArrayList;
        this.ipList = new File(new File(crummerCraft.startFile.getParentFile(), "config"), "offline-ip-list.txt");
        //TODO Potential issue
        this.list_ips = servers.stream().sorted(Comparator.comparingInt(Server::port)).map(Server::toString).collect(Collectors.joining("\n"));
        this.list_servers = serverJsonArray.stream().map(ServerJson::toStringWithoutPath).collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        new Config();
        createPath();
        jsonRun();
        fillArrayList();
        findMainServer();
        findProxy();
        JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.OFFLINE)
                .addEventListeners(new Main())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
    }

    public static void answer(MessageReceivedEvent e, String message) {
        if (e.getMember() == null) {
            System.out.println("This user didn't receive this message:" + e.getAuthor().getName());
            System.out.println(message);
        }
        e.getChannel().sendMessage(message).submit();
    }

    @Override
    public synchronized void onMessageReceived(@NotNull MessageReceivedEvent e) {
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
Welcome to CCBot V3.0.0, How may I assist you?
!ip - #Get the server status and player count
!list - #Get status for each server
!servers - #Lists all servers
!myip - #Find what your current IP is
!uuid - #Find out how to obtain your UUID
!web - #Gives the link to the CC website
!perf - #Gives performance tips for Minecraft
!CC help - #List more advanced features");
```""");
            case "!list" -> status(e);
            case "!ip" -> answer(e, "```yaml\nServer IPs:\n" + list_ips + "```");
            case "!myip" -> answer(e, "You can get your ip using: https://api64.ipify.org/");
            case "!servers" -> answer(e, "```yaml\nServers:\n" + list_servers + "```");
            case "!web" -> answer(e, website);
            case "!uuid" -> {
                try {
                    answer(e, findUUID(e));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            case "!perf" -> answer(e, """ 
```yaml
CCBot's Performance Tuning Tips!
Type these commands to go to the page you want:

!args - #Get tuning tips for the Java Virtual Machine
!fabric - #Explains how to install fabric
!mods - #Get a list of recommended fabric performance mods
```""");
            case "!args" -> answer(e, """ 
```yaml
A Guide to Minecraft JVM Arguments
----------------------------------
Step 1 - Find a good JDK
A JDK in simple terms is a Java distribution. CorrettoJDK is currently one of the best <https://aws.amazon.com/corretto/?filtered-posts.sort-by=item.additionalFields.createdDate&filtered-posts.sort-order=desc>
Just extract it and set your Java path to it in the Minecraft Launcher.

Step 2 - Find the args
The args section is found under a version profile. Click installations -> The 3 dots next to your profile ->  Edit -> More Options.
Once there, find the  JVM Arguments section. Do note that I am assuming you have around 8GB of ram in your machine, change the Xmx variable to represent your machine's ram. Here you have some options for arguments to past in there:
-------------------------------------------------
#G1GC - For older hardware

-Xms4G -Xmx4G -Xmn768m -XX:+AggressiveOpts -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+ParallelRefProcEnabled -XX:+PerfDisableSharedMem -XX:+UseCompressedOops -XX:-UsePerfData -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=50 -XX:G1HeapRegionSize=1 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=8
-------------------------------------------------
#Shenandoah - For more modern hardware

-Xms4G -Xmx4G -Xmn768m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:+UseNUMA -XX:+AlwaysPreTouch -XX:-UseBiasedLocking -XX:+DisableExplicitGC -Dfile.encoding=UTF-8
-------------------------------------------------
#If you use Linux, add these args to any of the ones above:
-XX:+UseLargePages -XX:LargePageSizeInBytes=2M
```""");
            case "!fabric" -> answer(e, """ 
```yaml
Installing Fabric is easy:
1. Download fabric at https://fabricmc.net/use/installer/
2. Run the installer while the MC launcher is closed
3. Select the version of MC you want and click install
4. Open the Launcher and it should appear
5. To access the mods folder on Windows, press Win+R, then type %appdata%/.minecraft
6. Locate a folder called "mods". If it doesn't exist, create it
```""");
            case "!mods" -> answer(e, """
```yaml
A good place to find lots of performance mods is:https://modrinth.com/mods?f=categories%3A%27optimization%27&g=categories%3A%27fabric%27&e=client
The main ones are:
Sodium: https://modrinth.com/mod/sodium
Lithium: https://modrinth.com/mod/lithium
LazyDFU: https://modrinth.com/mod/lazydfu
Krypton: https://modrinth.com/mod/krypton
FerriteCore: https://modrinth.com/mod/ferrite-core
EntityCulling: https://www.curseforge.com/minecraft/mc-mods/entityculling
ImmediateFast: https://modrinth.com/mod/immediatelyfast
Exordium: https://modrinth.com/mod/exordium
ForgetMeChunk: https://modrinth.com/mod/forgetmechunk
MoreCulling: https://modrinth.com/mod/moreculling
Enhanced Block Entities: https://modrinth.com/mod/ebe
Better Beds: https://modrinth.com/mod/better-beds

Additional mods you'll need:
Fabric API: https://modrinth.com/mod/fabric-api
Indium: https://modrinth.com/mod/indium
```""");
        }
if (message.startsWith("!CC") || message.startsWith("@CC")) {
            if (e.getMember().getRoles().stream().noneMatch(role -> role.getName().equals(admin_role))) {
                answer(e, "You must be an admin to use these commands!");
            } else {
                try {
                    handleAdmin(e);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
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
                """ + servers.stream().map(Server::status).collect(Collectors.joining("\n")) + "```");
    }

    private void handleAdmin(MessageReceivedEvent e) throws InterruptedException {
        final String[] s = e.getMessage().getContentDisplay().substring(4).split(" ");
        final String action = s[0];
        final String name = s.length == 1 ? null : s[1];
// CCBot's main functions

        switch (action.toLowerCase()) {
            case "start" -> answer(e, findServer(e, name).map(server -> server.start(e)).collect(Collectors.joining("\n")));
            case "stop" -> answer(e, findServer(e, name).map(Server::stop).collect(Collectors.joining("\n")));
            case "kill" -> findServer(e, name).forEach(server -> server.kill(e));
            case "status" -> status(e);
            case "exec" -> findServer(e, name).forEach(server -> server.exec(e, Arrays.stream(s).skip(2).collect(Collectors.joining(" "))));
            case "ignore_if_busy" -> findServer(e, name).forEach(server -> server.ignore_if_busy(e));
            case "external_stop" -> externalStop(e, name);
            case "external_kill" -> externalKill(e, name);
            case "list_running_jars" -> listRunningJars(e);
            case "pid" -> findServer(e, name).forEach(server -> server.pid(e));
// Logs
            case "start_tmp_logs" -> findServer(e, name).forEach(server -> server.startRealLogs(e));
            case "tmp_logs" -> findServer(e, name).forEach(server -> server.realLogs(e));
            case "stop_tmp_logs" -> findServer(e, name).forEach(server -> server.stopRealLogs(e));
            case "clear_tmp_logs" -> findServer(e, name).forEach(Server::clearRealLogs);
            case "logs" -> findServer(e, name).forEach(server -> server.readLatestLog(e));

// IP
            case "iplist" -> readIps(e);
            case "setip" -> writeIps(e, Arrays.stream(s).skip(1).collect(Collectors.joining(" ")));
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
                    Welcome to CCBot 3.0.0, An OpenSource Server Management Companion!
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
                    - kill [server] #kills servers; only do if needed
                    - status #shows current status of each server
                    - exec <server> #executes commands in-game
                    - ignore_if_busy [server] #override other active commands
                    - external_stop <pid> #send a stop signal to a java process
                    - external_kill <pid> #kill a java process
                    - list_running_jars #lists all running java processes
                    - pid [server] #shows the server's process id
                    ```""");
            case "help-log" -> answer(e, """
                    ```yaml
                    Try !CC with:
                    - start_tmp_logs [server] #start recoding the server's logs
                    - tmp_logs [server] #gives the temporary saved logs
                    - stop_tmp_logs [server] #stop recoding the server's logs
                    - clear_tmp_logs [server] #clears the temporary saved logs
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
            ioException.printStackTrace();
            answer(e, "An IO error occurred while reading!");
            return null;
        }
    }

    private void writeIpList(MessageReceivedEvent e, byte[] value) {
        try (OutputStream stream = new FileOutputStream(ipList)) {
            stream.write(value);
            stream.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
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
                .collect(Collectors.joining("\n")).getBytes();

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
        System.out.println(e.getMessage().getContentDisplay());
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
                .collect(Collectors.joining("\n")).getBytes();
        writeIpList(e, newValue);
    }

    private boolean notOwner(MessageReceivedEvent e) {
        final Member member = e.getMember();
        assert member != null;
        if (member.getRoles().stream().anyMatch(role -> role.getName().equals(owner_role))) {
            if (!private_channel.equals(e.getChannel().getName())) {
                answer(e, "You must execute this command in "+private_channel+".");
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
        final File ipList = new File(new File(crummerCraft.startFile.getParentFile(), "config"), "offline-ip-list.txt");
        try (OutputStream stream = new FileOutputStream(ipList)) {
            stream.write(message.getBytes());
            stream.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            answer(e, "An IO error occurred!");
            return;
        }
        answer(e, "File updated");
    }

    private void readIps(MessageReceivedEvent e) {
        if (notOwner(e)) {
            return;
        }

        final File ipList = new File(new File(crummerCraft.startFile.getParentFile(), "config"), "offline-ip-list.txt");
        e.getChannel().sendFiles(FileUpload.fromData(ipList)).submit();
    }

    // PID lister
    private void externalKill(MessageReceivedEvent e, String pid) {
        if (jarContains(pid)) {
            answer(e, "The process " + pid + " is going to be destroyed");
            try {
                Runtime.getRuntime().exec("/usr/bin/kill -9 " + pid);
            } catch (IOException ioException) {
                ioException.printStackTrace();
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
                Runtime.getRuntime().exec("/usr/bin/kill " + pid);
            } catch (IOException ioException) {
                ioException.printStackTrace();
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
            final Process process = Runtime.getRuntime().exec("/bin/ps -eo pid,comm,command");
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
            ioException.printStackTrace();
        }
    }

    private boolean jarContains(String pid) {
        try {
            final Process process = Runtime.getRuntime().exec("/bin/ps -eo pid,comm");
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
            ioException.printStackTrace();
        }
        return false;
    }
}
