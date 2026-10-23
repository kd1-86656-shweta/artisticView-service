package com.artisticview.media;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.MediaUploadDto;
import com.artisticview.common.enums.MediaKind;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Media")
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final ImageUploadService imageUploadService;
    private final VideoUploadService videoUploadService;

    /** Upload an image; save the returned key on a profile (avatar / cover), event, community or post. */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MediaUploadDto> uploadImage(@RequestParam MediaKind kind, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageUploadService.upload(CurrentUser.id(), kind, file));
    }

    /** Upload a post video (MP4, MOV or WebM, up to 100 MB); save the returned key on the post. */
    @PostMapping(value = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<MediaUploadDto> uploadVideo(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(videoUploadService.upload(CurrentUser.id(), file));
    }
}
