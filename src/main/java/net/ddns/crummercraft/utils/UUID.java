package net.ddns.crummercraft.utils;

public class UUID {
    Data data;

    public UUID(Data data) {
        this.data = data;
    }

    static class Data {
        Player player;

        public Data(Player player) {
            this.player = player;
        }
        static class Player {
            private final String username;
            private final String id;
            private final String avatar;

            public Player(String username, String id, String avatar) {
                this.username = username;
                this.id = id;
                this.avatar = avatar;
            }

            public String getUsername() {
                return this.username;
            }

            public String getUUID() {
                return this.id;
            }

            public String getAvatar() {
                return this.avatar;
            }

        }
    }
}
