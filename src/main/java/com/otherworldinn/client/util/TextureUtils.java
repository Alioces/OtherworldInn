package com.otherworldinn.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class TextureUtils {
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SKIN_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Map<String, ResourceLocation> MOJANG_SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MOJANG_SKIN_PENDING = ConcurrentHashMap.newKeySet();

    public static List<ResourceLocation> findTexturesInFolder(String namespace, String path) {
        List<ResourceLocation> textures = new ArrayList<>();
        // 获取 ResourceManager
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return textures;

        ResourceManager manager = mc.getResourceManager();

        // 扫描指定路径下的所有 png 文件
        Map<ResourceLocation, ?> resources =
                manager.listResources(
                        path,
                        loc ->
                                loc.getNamespace().equals(namespace)
                                        && loc.getPath().endsWith(".png"));

        textures.addAll(resources.keySet());

        // 排序以确保顺序一致
        textures.sort(ResourceLocation::compareTo);

        return textures;
    }

    public static ResourceLocation getMojangSkinTexture(String playerName, ResourceLocation fallback) {
        if (playerName == null || playerName.isBlank()) {
            return fallback;
        }
        String key = playerName.trim().toLowerCase();
        ResourceLocation cached = MOJANG_SKIN_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (!MOJANG_SKIN_PENDING.add(key)) {
            return fallback;
        }
        Thread thread =
                new Thread(
                        () -> {
                            try {
                                fetchAndCacheMojangSkin(key);
                            } catch (Throwable ignored) {
                            } finally {
                                MOJANG_SKIN_PENDING.remove(key);
                            }
                        },
                        "otherworldinn-mojang-skin-" + key);
        thread.setDaemon(true);
        thread.start();
        return fallback;
    }

    private static void fetchAndCacheMojangSkin(String playerName) throws IOException {
        String profileResponse =
                readHttp("https://api.mojang.com/users/profiles/minecraft/" + playerName, 3000, 3000);
        String profileId = matchGroup(PROFILE_ID_PATTERN, profileResponse);
        if (profileId == null || profileId.isBlank()) {
            return;
        }
        String sessionResponse =
                readHttp(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + profileId,
                        4000,
                        4000);
        String valueBase64 = matchGroup(PROPERTY_VALUE_PATTERN, sessionResponse);
        if (valueBase64 == null || valueBase64.isBlank()) {
            return;
        }
        String decoded = new String(java.util.Base64.getDecoder().decode(valueBase64));
        String skinUrl = matchGroup(SKIN_URL_PATTERN, decoded);
        if (skinUrl == null || skinUrl.isBlank()) {
            return;
        }
        skinUrl = skinUrl.replace("\\/", "/");
        String safeName = playerName.replaceAll("[^a-z0-9_\\-]", "_");
        ResourceLocation textureId =
                ResourceLocation.fromNamespaceAndPath("otherworldinn", "mojang_skin/" + safeName);

        try (InputStream in = new URL(skinUrl).openStream()) {
            NativeImage image = NativeImage.read(in);
            if (image == null) {
                return;
            }
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft mc = Minecraft.getInstance();
            mc.execute(
                    () -> {
                        mc.getTextureManager().register(textureId, texture);
                        MOJANG_SKIN_CACHE.put(playerName, textureId);
                    });
        }
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
