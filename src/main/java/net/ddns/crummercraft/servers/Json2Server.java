package net.ddns.crummercraft.servers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Json2Server {
    public static Json2Server tester = new Json2Server();
    public static ArrayList<ServerJson> serverJsonArray;

    static {
        try {
            serverJsonArray = tester.readJSON();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void jsonRun() {
        try {
            if (serverJsonArray.isEmpty()) {
                ServerJson example = new ServerJson("Example", "1.19.2", "Example/Path", 25565, "127.0.0.1", true, false);
                tester.writeJSON(example);
                serverJsonArray = tester.readJSON();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void writeJSON(ServerJson serverJson) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().create();
        FileWriter writer = new FileWriter("servers/"+serverJson.getName()+".json");
        gson.toJson(serverJson, writer);
        writer.close();
    }

    private ArrayList<ServerJson> readJSON() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonElement json;
        ArrayList<ServerJson> servers = new ArrayList<>();
        List<Path> jsons = listJsons();
        for (Path path : jsons) {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(String.valueOf(path)))) {
                json = JsonParser.parseReader(bufferedReader);
                servers.add(gson.fromJson(json, ServerJson.class));
            }
        }
        return servers;
    }

    private static List<Path> listJsons(){
        List<Path> paths = new ArrayList<>();
        try {
            Files.walk(CreateServerFile.path).forEach(path -> {
                if (path.toFile().isFile()) {
                    paths.add(path);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return paths;
    }
}