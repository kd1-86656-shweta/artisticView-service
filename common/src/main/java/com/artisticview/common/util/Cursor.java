package com.artisticview.common.util;

import com.artisticview.common.exception.AppException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Opaque keyset cursor of (timestamp, id). Ordering is always "newest first", ties broken by id desc.
 */
public record Cursor(Instant ts, long id) {

    /** Start of a listing: everything is "older" than this. */
    public static final Cursor START = new Cursor(Instant.parse("9999-12-31T00:00:00Z"), Long.MAX_VALUE);

    public static Cursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return START;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return new Cursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), Long.parseLong(parts[1]));
        } catch (RuntimeException e) {
            throw AppException.badRequest("Invalid cursor");
        }
    }

    public String encode() {
        String raw = ts.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
