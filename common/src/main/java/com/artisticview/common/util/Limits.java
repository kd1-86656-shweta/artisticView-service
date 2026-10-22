package com.artisticview.common.util;

public final class Limits {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    private Limits() {
    }

    public static int pageSize(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
