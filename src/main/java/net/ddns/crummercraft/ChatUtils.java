package net.ddns.crummercraft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.net.URL;

public class ChatUtils {

    public static String findUUID(MessageReceivedEvent e) {
        try {
            return readJSON(e);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "That account doesn't exist, or you didn't specify one";
        }
    }

    public static String separateName(MessageReceivedEvent e) {
        return e.getMessage().getContentDisplay().replace("!uuid ", "");
    }

    public static String readJSON(MessageReceivedEvent e) throws IOException {
        final URL url = new URL("https://playerdb.co/api/player/minecraft/" + separateName(e));
        final ObjectReader reader = new ObjectMapper().readerFor(UUID.class);
        final UUID response = reader.readValue(url);
        return response.data.player.id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UUID(ChatUtils.UUID.Data data) {
        @JsonCreator
        private UUID(@JsonProperty("data") Data data) {
            this.data = data;
        }
        @JsonIgnoreProperties(ignoreUnknown = true)

        private record Data(UUID.Data.Player player) {
            @JsonCreator
            private Data(@JsonProperty("player") Player player) {
                this.player = player;
            }
            @JsonIgnoreProperties(ignoreUnknown = true)

            private record Player(String id) {
                @JsonCreator
                private Player(@JsonProperty("id") String id) {
                    this.id = id;
                }
            }
        }
    }
}
