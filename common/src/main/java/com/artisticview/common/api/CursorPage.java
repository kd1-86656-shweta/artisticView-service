package com.artisticview.common.api;

import java.util.List;
import java.util.function.Function;

/**
 * A page of results for cursor pagination. {@code nextCursor} is null when there are no more items.
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

    public <R> CursorPage<R> map(Function<T, R> mapper) {
        return new CursorPage<>(items.stream().map(mapper).toList(), nextCursor);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null);
    }
}
