package com.artisticview.media;

import java.io.InputStream;

/** Where uploaded files live. Local disk in development; an S3 implementation can replace it in production. */
public interface MediaStorage {

    void put(String key, byte[] bytes, String contentType);

    /** For large files (videos): copies the stream without holding it all in memory. */
    void put(String key, InputStream in, String contentType);
}
