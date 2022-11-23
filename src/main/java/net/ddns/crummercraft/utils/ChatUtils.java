package net.ddns.crummercraft.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class ChatUtils {

    public static String findUUID(MessageReceivedEvent e) throws IOException {
        if (readJSON(e)==null) {
            return "That account doesn't exist, or you didn't specify one";
        } else return readJSON(e).data.player.getUUID();
    }

    public static String separateName(MessageReceivedEvent e) {
        return e.getMessage().getContentDisplay().replace("!uuid ", "");
    }

    public static UUID readJSON(MessageReceivedEvent e) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonElement json;
        URL url = new URL("https://playerdb.co/api/player/minecraft/"+separateName(e));
        HttpURLConnection connection1 = (HttpURLConnection)url.openConnection();
        connection1.setRequestMethod("GET");
        connection1.connect();
        if (connection1.getResponseCode() == 400) {
            connection1.disconnect();
            return null;
        }
        URLConnection connection = url.openConnection();
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            json = JsonParser.parseReader(bufferedReader);
        }
        return gson.fromJson(json, UUID.class);
    }
}
