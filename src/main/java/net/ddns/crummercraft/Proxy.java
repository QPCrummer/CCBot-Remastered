package net.ddns.crummercraft;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Proxy extends Server {

    public Proxy(File startFile, String name, String ip, Consumer<MessageReceivedEvent> onStarting, Consumer<MessageReceivedEvent> onStopped) {
        super(startFile, name, ip, onStarting, onStopped);
    }

    @Override
    protected String listPlayers() throws InterruptedException {
        final Process localProcess = this.process;
        try {
            playerListSemaphore.acquire();
            localProcess.getOutputStream().write("glist\n".getBytes());
            localProcess.getOutputStream().flush();
        } catch (IOException error) {
            error.printStackTrace();
            playerListSemaphore.release();
            return "Failed to receive the list command!";
        }
        if (!playerListSemaphore.tryAcquire(10, TimeUnit.SECONDS)) {
            return "Failed to retrieve status!";
        } else {
            playerListSemaphore.release();
        }

        return "online #" + playerList.substring(17, playerList.indexOf(" player")) + "/∞ players";
    }

    @Override
    protected String stopSignal() {
        return "end\n";
    }
}
