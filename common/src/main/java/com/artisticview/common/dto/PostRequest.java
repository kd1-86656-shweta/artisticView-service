package com.artisticview.common.dto;

import jakarta.validation.constraints.Size;

/**
 * Create or edit a post. {@code communitySlug} (create only) posts into a community; leave it out to post on
 * your profile. On edit, null fields stay as they are and imageKey "" removes the image.
 */
public record PostRequest(
        @Size(max = 40) String communitySlug,
        @Size(max = 300) String title,
        @Size(max = 10000) String body,
        @Size(max = 300) String imageKey,
        /** From a video upload; "" removes it. A post carries an image or a video, not both. */
        @Size(max = 300) String videoKey) {
}
