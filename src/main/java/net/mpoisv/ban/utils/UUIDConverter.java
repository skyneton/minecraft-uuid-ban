package net.mpoisv.ban.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.util.UUIDTypeAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class UUIDConverter {
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String NAME_URL = "https://api.mojang.com/user/profile/";
    private static Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private String name;
    private UUID id;

    public static UUID getUUID(String name) throws IOException {
        var connection = (HttpURLConnection) new URL(UUID_URL + name).openConnection();
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        var data = gson.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDConverter.class);

        return data.id;
    }

    public static UUID getUUIDFromUUID(String uuid) {
        return UUIDTypeAdapter.fromString(uuid);
    }
}
