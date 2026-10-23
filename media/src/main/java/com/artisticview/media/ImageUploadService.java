package com.artisticview.media;

import com.artisticview.common.dto.MediaUploadDto;
import com.artisticview.common.enums.MediaKind;
import com.artisticview.common.exception.AppException;
import com.artisticview.core.storage.MediaUrls;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Accepts an image upload, checks it really is an image, and re-encodes it as a JPEG at the size its kind needs.
 * Re-encoding also strips metadata such as GPS location.
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final MediaStorage storage;
    private final MediaUrls mediaUrls;

    public MediaUploadDto upload(Long userId, MediaKind kind, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("Choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.badRequest("Images can be up to 8 MB");
        }
        if (file.getContentType() == null || !ALLOWED.contains(file.getContentType())) {
            throw AppException.badRequest("Use a JPG, PNG or WebP image");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            throw AppException.badRequest("That file is not an image we can read");
        }

        byte[] jpeg;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var builder = Thumbnails.of(image).outputFormat("jpg").outputQuality(0.85);
            if (kind.crop()) {
                builder.size(kind.width(), kind.height()).crop(Positions.CENTER);
            } else if (image.getWidth() > kind.width() || image.getHeight() > kind.height()) {
                builder.size(kind.width(), kind.height());
            } else {
                builder.scale(1.0);
            }
            builder.toOutputStream(out);
            jpeg = out.toByteArray();
        } catch (IOException e) {
            throw AppException.badRequest("Could not process that image");
        }

        String key = kind.folder() + "/" + userId + "/" + UUID.randomUUID() + ".jpg";
        storage.put(key, jpeg, "image/jpeg");
        return new MediaUploadDto(key, mediaUrls.url(key));
    }
}
