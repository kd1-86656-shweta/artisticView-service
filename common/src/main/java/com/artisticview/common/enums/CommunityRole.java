package com.artisticview.common.enums;

/** OWNER runs the community (one per community); MODERATORS manage members and remove posts. */
public enum CommunityRole {
    OWNER, MODERATOR, MEMBER;

    public boolean canModerate() {
        return this == OWNER || this == MODERATOR;
    }
}
