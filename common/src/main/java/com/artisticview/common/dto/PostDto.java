package com.artisticview.common.dto;

import java.time.Instant;

/**
 * A post as the viewer sees it. {@code community} is null for posts on a profile. {@code myVote} is
 * 1 (upvoted), -1 (downvoted) or 0.
 */
public record PostDto(Long id, UserSummaryDto author, CommunityRef community, String title, String body,
                      String imageUrl, String videoUrl, long score, int myVote, long commentCount, boolean canEdit, boolean canDelete,
                      Instant createdAt, Instant editedAt) {

    public record CommunityRef(String slug, String name) {
    }
}
