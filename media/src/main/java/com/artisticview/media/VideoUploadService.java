package com.artisticview.media;

import com.artisticview.common.dto.MediaUploadDto;
import com.artisticview.common.exception.AppException;
import com.artisticview.core.storage.MediaUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Stores a post video as uploaded (no transcoding yet). The file's first bytes must really be an MP4/MOV or WebM
 * container, whatever its name or declared type says.
 */
@Service
@RequiredArgsConstructor
public class VideoUploadService {

    public static final long MAX_BYTES = 100L * 1024 * 1024;
    private static final byte[] WEBM_MAGIC = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};

    private final MediaStorage storage;
    private final MediaUrls mediaUrls;

    public MediaUploadDto upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("Choose a video to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.badRequest("Videos can be up to 100 MB");
        }
        String ext = detect(file);
        if (ext == null) {
            throw AppException.badRequest("Use an MP4, MOV or WebM video");
        }
        String key = "video/" + userId + "/" + UUID.randomUUID() + "." + ext;
        try {
            storage.put(key, file.getInputStream(), "mp4".equals(ext) ? "video/mp4" : "video/webm");
        } catch (IOException e) {
            throw AppException.badRequest("Could not read that video");
        }
        return new MediaUploadDto(key, mediaUrls.url(key));
    }

    /** Video keys are "video/{userId}/{uuid}.{ext}", so ownership can be checked from the key alone. */
    public static boolean ownedBy(String key, Long userId) {
        return key != null && key.startsWith("video/" + userId + "/") && !key.contains("..");
    }

    /** "mp4" for MP4/MOV (an "ftyp" box at byte 4), "webm" for WebM/Matroska, null for anything else. */
    private static String detect(MultipartFile file) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            if (in.readNBytes(head, 0, head.length) < head.length) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        if (head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
            return "mp4";
        }
        return Arrays.equals(Arrays.copyOf(head, 4), WEBM_MAGIC) ? "webm" : null;
    }
}
