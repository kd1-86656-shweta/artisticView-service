package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.api.CursorPage;
import com.artisticview.common.dto.CommentDto;
import com.artisticview.common.dto.CommentRequest;
import com.artisticview.common.dto.PostDto;
import com.artisticview.common.dto.PostRequest;
import com.artisticview.common.dto.VoteRequest;
import com.artisticview.core.permission.StaffCheck;
import com.artisticview.core.post.PostService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Posts")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final StaffCheck staffCheck;

    /** community=slug, author=username, or neither for the viewer's feed. sort = hot (default) | new | top. */
    @GetMapping("/posts")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_VIEW')")
    public ApiResponse<CursorPage<PostDto>> list(@RequestParam(required = false) String community,
                                                 @RequestParam(required = false) String author,
                                                 @RequestParam(required = false) String sort,
                                                 @RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(postService.list(CurrentUser.id(), staffCheck.canModerateContent(), community, author, sort, cursor, size));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<PostDto> create(@Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.create(CurrentUser.id(), staffCheck.canModerateContent(), request));
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_VIEW')")
    public ApiResponse<PostDto> get(@PathVariable Long id) {
        return ApiResponse.ok(postService.get(id, CurrentUser.id(), staffCheck.canModerateContent()));
    }

    @PatchMapping("/posts/{id}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<PostDto> update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.update(id, CurrentUser.id(), staffCheck.canModerateContent(), request));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_VIEW')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.delete(id, CurrentUser.id(), staffCheck.canModerateContent());
        return ApiResponse.ok(null);
    }

    @PutMapping("/posts/{id}/vote")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<PostDto> vote(@PathVariable Long id, @Valid @RequestBody VoteRequest request) {
        return ApiResponse.ok(postService.vote(id, CurrentUser.id(), staffCheck.canModerateContent(), request.value()));
    }

    @GetMapping("/posts/{id}/comments")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_VIEW')")
    public ApiResponse<List<CommentDto>> comments(@PathVariable Long id) {
        return ApiResponse.ok(postService.comments(id, CurrentUser.id(), staffCheck.canModerateContent()));
    }

    @PostMapping("/posts/{id}/comments")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<CommentDto> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(postService.addComment(id, CurrentUser.id(), staffCheck.canModerateContent(), request));
    }

    @PatchMapping("/comments/{id}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_CREATE')")
    public ApiResponse<CommentDto> editComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(postService.editComment(id, CurrentUser.id(), staffCheck.canModerateContent(), request.body()));
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'POST_VIEW')")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        postService.deleteComment(id, CurrentUser.id(), staffCheck.canModerateContent());
        return ApiResponse.ok(null);
    }
}
