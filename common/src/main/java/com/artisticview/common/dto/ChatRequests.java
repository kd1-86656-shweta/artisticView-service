package com.artisticview.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request bodies for the chat API. */
public final class ChatRequests {

    private ChatRequests() {
    }

    /** Open (or reopen) a one-to-one chat with this person. */
    public record Direct(@NotBlank @Size(max = 30) String username) {
    }

    /** Start a group with these people; the creator becomes an admin. */
    public record CreateGroup(@NotBlank @Size(max = 80) String title,
                              @Size(min = 1, max = 100) List<@Size(max = 30) String> usernames) {
    }

    /** Rename a group or change its picture (avatarKey "" removes it). */
    public record UpdateGroup(@Size(min = 1, max = 80) String title, @Size(max = 300) String avatarKey) {
    }

    /** Text, a photo (POST image upload key), or both. */
    public record Send(@Size(max = 4000) String body, @Size(max = 300) String imageKey) {
    }

    public record Read(@NotNull Long messageId) {
    }
}
