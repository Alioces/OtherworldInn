package com.otherworldinn.world.dialogue;

public record LocalizedText(String zh, String en) {
    public static LocalizedText of(String zh, String en) {
        return new LocalizedText(zh, en);
    }
}
