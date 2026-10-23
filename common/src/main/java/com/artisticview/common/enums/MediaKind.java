package com.artisticview.common.enums;

/**
 * What an uploaded image is for. Each kind has its own output size: avatars and covers are cropped to fit,
 * post images keep their shape and are only scaled down.
 */
public enum MediaKind {
    AVATAR("avatar", 512, 512, true),
    COVER("cover", 1600, 600, true),
    EVENT("event", 1600, 900, true),
    COMMUNITY("community", 1600, 500, true),
    POST("post", 1600, 1600, false);

    private final String folder;
    private final int width;
    private final int height;
    private final boolean crop;

    MediaKind(String folder, int width, int height, boolean crop) {
        this.folder = folder;
        this.width = width;
        this.height = height;
        this.crop = crop;
    }

    public String folder() {
        return folder;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean crop() {
        return crop;
    }

    /** Keys are "{folder}/{userId}/{uuid}.jpg", so ownership can be checked from the key alone. */
    public boolean ownedBy(String key, Long userId) {
        return key != null && key.startsWith(folder + "/" + userId + "/") && !key.contains("..");
    }
}
