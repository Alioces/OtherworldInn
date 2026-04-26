package com.otherworldinn.util.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;

public final class MojangProfileService {
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MODEL_PATTERN = Pattern.compile("\"model\"\\s*:\\s*\"(slim|classic)\"");

    private static final Map<String, Boolean> MODEL_TYPE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();

    private MojangProfileService() {}

    public static void resolveSlimModelAsync(
            String playerName, MinecraftServer server, Consumer<Boolean> callback) {
        if (playerName == null || playerName.isBlank() || server == null) {
            return;
        }
        String key = playerName.trim().toLowerCase();
        Boolean cached = MODEL_TYPE_CACHE.get(key);
        if (cached != null) {
            server.execute(() -> callback.accept(cached));
            return;
        }
        if (!PENDING.add(key)) {
            return;
        }
        Thread thread =
                new Thread(
                        () -> {
                            try {
                                boolean slim = fetchSlimModel(key);
                                MODEL_TYPE_CACHE.put(key, slim);
                                server.execute(() -> callback.accept(slim));
                            } catch (Throwable ignored) {
                            } finally {
                                PENDING.remove(key);
                            }
                        },
                        "otherworldinn-mojang-model-" + key);
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean fetchSlimModel(String playerName) throws IOException {
        String profileResponse =
                readHttp("https://api.mojang.com/users/profiles/minecraft/" + playerName, 3000, 3000);
        String profileId = matchGroup(PROFILE_ID_PATTERN, profileResponse);
        if (profileId == null || profileId.isBlank()) {
            return false;
        }
        String sessionResponse =
                readHttp(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + profileId,
                        4000,
                        4000);
        String valueBase64 = matchGroup(PROPERTY_VALUE_PATTERN, sessionResponse);
        if (valueBase64 == null || valueBase64.isBlank()) {
            return false;
        }
        String decoded = new String(java.util.Base64.getDecoder().decode(valueBase64));
        String model = matchGroup(MODEL_PATTERN, decoded);
        return "slim".equalsIgnoreCase(model);
    }

    private static String readHttp(String url, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setUseCaches(false);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Unexpected HTTP status: " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes());
        } finally {
            conn.disconnect();
        }
    }

    private static String matchGroup(Pattern pattern, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find() || matcher.groupCount() < 1) {
            return null;
        }
        return matcher.group(1);
    }
}
