package net.ddns.crummercraft.servers;

import gs.mclo.api.Log;
import gs.mclo.api.MclogsClient;
import gs.mclo.api.response.UploadLogResponse;
import net.ddns.crummercraft.Main;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server{

    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected final File startFile;
    protected final Consumer<MessageReceivedEvent> onStarting;
    protected final Consumer<MessageReceivedEvent> onStopped;
    protected final Semaphore semaphore = new Semaphore(1);
    protected final Semaphore playerListSemaphore = new Semaphore(1);
    private final String name;
    private final String ip;
    private final ByteArrayOutputStream realLogs = new ByteArrayOutputStream();
    private final AtomicBoolean realLogging = new AtomicBoolean(false);
    private final String minecraftVersion;
    public final ServerData info;
    protected Process process;
    protected String playerList;
    private String pid;
    private final int port;
    private final Path path;

    public Server(ServerData info, Consumer<MessageReceivedEvent> onStarting, Consumer<MessageReceivedEvent> onStopped) {
        String os = System.getProperty("os.name");
        String start_file;
        if (os.contains("Windows")) {
            start_file = "/start.bat";
        } else {
            start_file = "/start.sh";
        }
        this.startFile = new File(info.serverFolder()+start_file);
        this.name = info.name();
        this.ip = info.serverIP();
        this.port = info.serverPort();
        this.info = info;
        this.minecraftVersion = info.minecraftVersion();
        this.onStarting = onStarting;
        this.onStopped = onStopped;
        this.path = Paths.get(info.serverFolder());
    }

    public String name() {
        return name;
    }

    protected boolean tryAcquire(MessageReceivedEvent e) {
        if (!semaphore.tryAcquire()) {
            log(e, " is handling another command. Please wait or do !CC override");
            return false;
        }
        return true;
    }

    protected String listPlayers() throws InterruptedException {
        final Process localProcess = this.process;
        try {
            playerListSemaphore.acquire();
            localProcess.getOutputStream().write("list\n".getBytes());
            localProcess.getOutputStream().flush();
        } catch (IOException error) {
            Main.LOGGER.error("Failed to receive the player list", error);
            playerListSemaphore.release();
            return "Failed to receive the list command!";
        }
        if (!playerListSemaphore.tryAcquire(10, TimeUnit.SECONDS)) {
            return "Failed to retrieve status!";
        } else {
            playerListSemaphore.release();
        }

        final String end = playerList.substring(playerList.indexOf(": There are ") + 12);
        final String playerCount = end.substring(0, end.indexOf(" of a max of "));
        final String max = end.substring(end.indexOf(" of a max of ") + 13);
        final String maxPlayerCount = max.substring(0, max.indexOf(" players online:"));
        final String playerNames = max.substring(max.indexOf(" players online:") + 16);
        return "online #" + playerCount + "/" + maxPlayerCount + " players:" + playerNames;
    }

    protected String stopSignal() {
        return "stop\n";
    }

    private String startAction(MessageReceivedEvent e) {
        try {
            process = Runtime.getRuntime().exec(new String[]{startFile.getAbsolutePath()});
            new Thread(() -> readLogs(e)).start();
        } catch (IOException error) {
            Main.LOGGER.error("Failed to start server", error);
            return name + " startup failed!";
        }
        onStarting.accept(e);
        return name + " is starting";
    }

    private String stopAction() {
        try {
            process.getOutputStream().write(stopSignal().getBytes());
            process.getOutputStream().flush();
        } catch (IOException error) {
            Main.LOGGER.error("Failed to stop server", error);
            return name + " didn't received the stop command!";
        }
        return name + " is shutting down";
    }

    private String restartAction(MessageReceivedEvent e) {
        stopAction();
        Thread restart = new Thread(() -> waitForShutdown(e));
        restart.start();
        waitForShutdown(e);
        restart.interrupt();
        return startAction(e);
    }

    public String start(MessageReceivedEvent e) {
        if (!semaphore.tryAcquire()) {
            return name + " is busy";
        }
        if (running.getAndSet(true)) {
            semaphore.release();
            return name + " is already running";
        }
        return startAction(e);
    }

    public String stop() {
        if (!semaphore.tryAcquire()) {
            return name + " is busy";
        }
        if (!running.getAndSet(false)) {
            semaphore.release();
            return name + " is already down";
        }
        return stopAction();
    }

    public void override(MessageReceivedEvent e) {
        semaphore.release();
        log(e, " can now execute tasks simultaneously");
    }

    public String restart(MessageReceivedEvent e) {
        if (!semaphore.tryAcquire()) {
            return name + " is busy";
        }
        if (!running.getAndSet(false)) {
            semaphore.release();
            return startAction(e);
        }
        return restartAction(e);
    }

    public void kill(MessageReceivedEvent e) {
        if (!tryAcquire(e)) {
            return;
        }
        if (!running.getAndSet(false)) {
            semaphore.release();
            log(e, " is already down");
            return;
        }
        log(e, "'s current pid is " + this.pid + " and is going to be DESTROYED");
        try {
            Runtime.getRuntime().exec(new String[]{"/usr/bin/kill", "-9", this.pid});
        } catch (IOException error) {
            Main.LOGGER.error("Failed to kill server", error);
            log(e, "Couldn't destroyed the server!");
        }
        this.pid = null;
    }

    protected void readLogs(MessageReceivedEvent e) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            this.pid = reader.readLine();

            if (realLogging.get()) {
                realLogs.write((pid + "\n").getBytes());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (realLogging.get()) {
                    realLogs.write((line + "\n").getBytes());
                }
                //Darn Geyser messing everything up :(
                if (line.contains(": Done (") && !line.contains("/geyser")) {
                    semaphore.release();
                    final String part = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
                    e.getChannel().sendMessage("```diff\n+ " + name + " started in " + part + "\n```").submit();
                } else if (line.contains(": There are ")) {
                    playerListSemaphore.release();
                    playerList = line;
                } else if (line.contains("currently connected to the proxy.")) {
                    playerListSemaphore.release();
                    playerList = line;
                } else if (line.contains(" joined the game")) {
                    e.getChannel().sendMessage("```" + name + ": " + extractPlayerName(line) + " joined```").submit();
                } else if (line.contains(" left the game")) {
                    e.getChannel().sendMessage("```" + name + ": " + extractPlayerName(line) + " left```").submit();
                }
            }
        } catch (IOException error) {
            Main.LOGGER.error("Failed to read logs", error);
            log(e, "'s logs weren't produced!");
        } finally {
            waitForShutdown(e);
            onStopped.accept(e);
            running.set(false);
            semaphore.release();
            this.pid = null;
            e.getChannel().sendMessage("```diff\n- " + name + " has stopped\n```").submit();
        }
    }

    private String extractPlayerName(String logMessage) {
        String regex = "\\[Server thread/INFO\\] \\(Minecraft\\) (\\w+) joined the game";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(logMessage);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "Unknown";
    }

    private void waitForShutdown(MessageReceivedEvent e) {
        try {
            Thread.sleep(200);
            while (process.isAlive()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException error) {
            Main.LOGGER.warn("Shutdown interrupted");
            log(e, "'s shutdown has been interrupted!");
        }
    }

    protected void log(MessageReceivedEvent e, String message) {
        e.getChannel().sendMessage(name + message).submit();
    }

    @Override
    public String toString() {
        return " - " + name + ":  " + ip + " ("+this.minecraftVersion+")";
    }

    public boolean isRunning() {
        return running.get();
    }

    public int port() {
        return port;
    }

    public void pid(MessageReceivedEvent e) {
        log(e, "'s pid is " + pid);
    }

    public void readLatestLog(MessageReceivedEvent e) {
        Path log_path = Paths.get(path + "/logs");
        Log log = new Log(log_path + "/latest.log");
        MclogsClient client = new MclogsClient("CCBot", "3.2.0", this.minecraftVersion);
        CompletableFuture<UploadLogResponse> future = client.uploadLog(log);
        try {
            UploadLogResponse response = future.get();
            e.getChannel().sendMessage("Here is the latest log: " + response.getUrl()).submit();
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void exec(MessageReceivedEvent e, String command) {
        if (!tryAcquire(e)) {
            return;
        }
        if (!running.get()) {
            semaphore.release();
            log(e, " is down");
            return;
        }
        try {
            process.getOutputStream().write((command + "\n").getBytes());
            process.getOutputStream().flush();
        } catch (IOException error) {
            Main.LOGGER.error("Failed to execute command", error);
            log(e, " didn't received the command: " + command);
        }
        semaphore.release();
    }

    public void clearLogs(MessageReceivedEvent e) {
        Path log_path = Paths.get(path + "/logs");
        int counter = 0;
        for(File file: Objects.requireNonNull(log_path.toFile().listFiles())) {
            if (!file.getName().equals("latest.log")) {
                file.delete();
                counter++;
            }
        }
        e.getChannel().sendMessage("Cleared " + counter + " log files").submit();
    }

    public synchronized String status() {
        if (semaphore.availablePermits() == 0) {
            return " - " + name + ": busy";
        } else if (!running.get()) {
            return " - " + name + ": offline";
        }
        try {
            return " - " + name + ": " + listPlayers();
        } catch (InterruptedException e) {
            Main.LOGGER.error("Failed to obtain server status", e);
            return " - " + name + ": status command interrupted";
        }
    }
}
