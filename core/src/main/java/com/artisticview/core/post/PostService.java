package com.artisticview.core.post;

import com.artisticview.common.api.CursorPage;
import com.artisticview.common.dto.CommentDto;
import com.artisticview.common.dto.CommentRequest;
import com.artisticview.common.dto.PostDto;
import com.artisticview.common.dto.PostRequest;
import com.artisticview.common.dto.UserSummaryDto;
import com.artisticview.common.enums.CommunityVisibility;
import com.artisticview.common.enums.MediaKind;
import com.artisticview.common.exception.AppException;
import com.artisticview.common.util.Limits;
import com.artisticview.core.community.CommunityService;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.core.user.UserSummaries;
import com.artisticview.persistence.community.Community;
import com.artisticview.persistence.community.CommunityMember;
import com.artisticview.persistence.community.CommunityMemberRepository;
import com.artisticview.persistence.community.CommunityRepository;
import com.artisticview.persistence.post.Post;
import com.artisticview.persistence.post.PostComment;
import com.artisticview.persistence.post.PostCommentRepository;
import com.artisticview.persistence.post.PostRepository;
import com.artisticview.persistence.post.PostVote;
import com.artisticview.persistence.post.PostVoteRepository;
import com.artisticview.persistence.social.BlockRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reddit-style posts: in a community (members only) or on the author's profile, with an image or a video,
 * up/down votes and threaded comments. Authors edit their own posts and comments; authors, community
 * moderators and platform moderators can delete them.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Set<String> SORTS = Set.of("hot", "new", "top");
    private static final int MAX_COMMENTS = 1000;

    private final PostRepository postRepository;
    private final PostVoteRepository voteRepository;
    private final PostCommentRepository commentRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityService communityService;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final UserSummaries userSummaries;
    private final MediaUrls mediaUrls;

    // ------------------------------------------------------------------ listing

    /**
     * Posts for a community ({@code communitySlug}), a person ({@code authorUsername}) or, with neither, the
     * viewer's feed. The cursor is the page number.
     */
    @Transactional(readOnly = true)
    public CursorPage<PostDto> list(Long viewerId, boolean staff, String communitySlug, String authorUsername,
                                    String sort, String cursor, Integer size) {
        int limit = Limits.pageSize(size);
        int page = cursor == null || cursor.isBlank() ? 0 : parsePage(cursor);
        String order = sort == null || !SORTS.contains(sort) ? "hot" : sort;
        Long communityId = null;
        Long authorId = null;
        if (communitySlug != null && !communitySlug.isBlank()) {
            Community c = communityService.load(communitySlug);
            requireCanRead(c, viewerId);
            communityId = c.getId();
        }
        if (authorUsername != null && !authorUsername.isBlank()) {
            authorId = userRepository.findByUsername(authorUsername.toLowerCase(Locale.ROOT)).filter(User::isActive)
                    .map(User::getId).orElseThrow(() -> AppException.notFound("Profile"));
        }
        boolean feed = communityId == null && authorId == null;
        List<Post> rows = postRepository.list(viewerId, communityId, authorId, feed, order, limit + 1, page * limit);
        boolean more = rows.size() > limit;
        List<Post> items = more ? rows.subList(0, limit) : rows;
        return new CursorPage<>(toDtos(items, viewerId, staff), more ? String.valueOf(page + 1) : null);
    }

    @Transactional(readOnly = true)
    public PostDto get(Long postId, Long viewerId, boolean staff) {
        return toDtos(List.of(loadReadable(postId, viewerId)), viewerId, staff).get(0);
    }

    // ------------------------------------------------------------------ create / edit / delete

    @Transactional
    public PostDto create(Long authorId, boolean staff, PostRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw AppException.badRequest("Give your post a title");
        }
        Post post = new Post();
        post.setAuthorId(authorId);
        if (request.communitySlug() != null && !request.communitySlug().isBlank()) {
            Community c = communityService.load(request.communitySlug());
            CommunityMember me = communityService.membership(c.getId(), authorId);
            if (me == null || !me.isActive()) {
                throw AppException.forbidden("Join the community to post in it");
            }
            post.setCommunityId(c.getId());
        }
        post.setTitle(request.title().trim());
        post.setBody(blankToNull(request.body()));
        applyMedia(post, request, authorId);
        postRepository.save(post);
        if (post.getCommunityId() != null) {
            communityRepository.adjustPosts(post.getCommunityId(), 1);
        }
        return get(post.getId(), authorId, staff);
    }

    @Transactional
    public PostDto update(Long postId, Long viewerId, boolean staff, PostRequest request) {
        Post post = load(postId);
        if (!Objects.equals(post.getAuthorId(), viewerId)) {
            throw AppException.forbidden("Only the author can edit this post");
        }
        if (request.title() != null) {
            if (request.title().isBlank()) throw AppException.badRequest("The title cannot be empty");
            post.setTitle(request.title().trim());
        }
        if (request.body() != null) post.setBody(blankToNull(request.body()));
        applyMedia(post, request, viewerId);
        post.setEditedAt(Instant.now());
        postRepository.save(post);
        return get(postId, viewerId, staff);
    }

    @Transactional
    public void delete(Long postId, Long viewerId, boolean staff) {
        Post post = load(postId);
        if (!canDelete(post.getAuthorId(), post.getCommunityId(), viewerId, staff, moderatedBy(viewerId, post.getCommunityId()))) {
            throw AppException.forbidden("You cannot delete this post");
        }
        postRepository.delete(post);
        if (post.getCommunityId() != null) {
            communityRepository.adjustPosts(post.getCommunityId(), -1);
        }
    }

    /** A post carries an image or a video, not both; "" removes. New media must be uploaded by the author. */
    private void applyMedia(Post post, PostRequest r, Long userId) {
        if (r.imageKey() != null) {
            if (!r.imageKey().isBlank() && !r.imageKey().equals(post.getImageKey()) && !MediaKind.POST.ownedBy(r.imageKey(), userId)) {
                throw AppException.badRequest("Upload the image again and retry");
            }
            post.setImageKey(blankToNull(r.imageKey()));
        }
        if (r.videoKey() != null) {
            boolean ownVideo = r.videoKey().startsWith("video/" + userId + "/") && !r.videoKey().contains("..");
            if (!r.videoKey().isBlank() && !r.videoKey().equals(post.getVideoKey()) && !ownVideo) {
                throw AppException.badRequest("Upload the video again and retry");
            }
            post.setVideoKey(blankToNull(r.videoKey()));
        }
        if (post.getImageKey() != null && post.getVideoKey() != null) {
            throw AppException.badRequest("Add an image or a video, not both");
        }
    }

    // ------------------------------------------------------------------ votes

    /** 1 = upvote, -1 = downvote, 0 = clear. The score changes by the difference from the previous vote. */
    @Transactional
    public PostDto vote(Long postId, Long viewerId, boolean staff, int value) {
        loadReadable(postId, viewerId);
        Short previous = voteRepository.findValue(postId, viewerId);
        int before = previous == null ? 0 : previous;
        if (before != value) {
            if (value == 0) voteRepository.remove(postId, viewerId);
            else voteRepository.upsert(postId, viewerId, (short) value);
            postRepository.adjustScore(postId, value - before);
        }
        return get(postId, viewerId, staff);
    }

    // ------------------------------------------------------------------ comments

    @Transactional(readOnly = true)
    public List<CommentDto> comments(Long postId, Long viewerId, boolean staff) {
        Post post = loadReadable(postId, viewerId);
        List<PostComment> rows = commentRepository.findThread(postId, Limit.of(MAX_COMMENTS));
        Set<Long> blocked = blockedAmong(viewerId, rows.stream().map(PostComment::getAuthorId).collect(Collectors.toSet()));
        Map<Long, UserSummaryDto> people = userSummaries.of(rows.stream().map(PostComment::getAuthorId).collect(Collectors.toSet()));
        boolean moderator = moderatedBy(viewerId, post.getCommunityId());
        return rows.stream().map(c -> toDto(c, people, viewerId, staff, moderator, post, blocked)).toList();
    }

    @Transactional
    public CommentDto addComment(Long postId, Long viewerId, boolean staff, CommentRequest request) {
        Post post = loadReadable(postId, viewerId);
        if (post.getCommunityId() != null) {
            CommunityMember me = communityService.membership(post.getCommunityId(), viewerId);
            if (me == null || !me.isActive()) {
                throw AppException.forbidden("Join the community to comment");
            }
        }
        if (request.parentId() != null) {
            PostComment parent = commentRepository.findById(request.parentId())
                    .filter(p -> p.getPostId().equals(postId))
                    .orElseThrow(() -> AppException.badRequest("That comment is not on this post"));
            if (parent.isDeleted()) {
                throw AppException.badRequest("You cannot reply to a deleted comment");
            }
        }
        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setAuthorId(viewerId);
        c.setParentId(request.parentId());
        c.setBody(request.body().trim());
        commentRepository.save(c);
        postRepository.adjustComments(postId, 1);
        Map<Long, UserSummaryDto> people = userSummaries.of(List.of(viewerId));
        return toDto(c, people, viewerId, staff, moderatedBy(viewerId, post.getCommunityId()), post, Set.of());
    }

    @Transactional
    public CommentDto editComment(Long commentId, Long viewerId, boolean staff, String body) {
        PostComment c = commentRepository.findById(commentId).orElseThrow(() -> AppException.notFound("Comment"));
        if (!Objects.equals(c.getAuthorId(), viewerId) || c.isDeleted()) {
            throw AppException.forbidden("Only the author can edit this comment");
        }
        if (body == null || body.isBlank()) {
            throw AppException.badRequest("A comment cannot be empty");
        }
        c.setBody(body.trim());
        c.setEditedAt(Instant.now());
        commentRepository.save(c);
        Post post = load(c.getPostId());
        return toDto(c, userSummaries.of(List.of(viewerId)), viewerId, staff, moderatedBy(viewerId, post.getCommunityId()), post, Set.of());
    }

    /** Soft delete: the comment stays as "[deleted]" so replies keep their place. */
    @Transactional
    public void deleteComment(Long commentId, Long viewerId, boolean staff) {
        PostComment c = commentRepository.findById(commentId).orElseThrow(() -> AppException.notFound("Comment"));
        if (c.isDeleted()) {
            return;
        }
        Post post = load(c.getPostId());
        if (!canDelete(c.getAuthorId(), post.getCommunityId(), viewerId, staff, moderatedBy(viewerId, post.getCommunityId()))) {
            throw AppException.forbidden("You cannot delete this comment");
        }
        c.setDeleted(true);
        c.setBody("");
        commentRepository.save(c);
        postRepository.adjustComments(post.getId(), -1);
    }

    // ------------------------------------------------------------------ helpers

    private Post load(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> AppException.notFound("Post"));
    }

    /** Loads a post the viewer may see: not hidden by a block, and from a community they can read. */
    private Post loadReadable(Long postId, Long viewerId) {
        Post post = load(postId);
        if (!Objects.equals(post.getAuthorId(), viewerId) && blockRepository.existsBetween(viewerId, post.getAuthorId())) {
            throw AppException.notFound("Post");
        }
        if (post.getCommunityId() != null) {
            requireCanRead(communityRepository.findById(post.getCommunityId()).orElseThrow(), viewerId);
        }
        return post;
    }

    private void requireCanRead(Community c, Long viewerId) {
        if (c.getVisibility() == CommunityVisibility.PRIVATE) {
            CommunityMember me = communityService.membership(c.getId(), viewerId);
            if (me == null || !me.isActive()) {
                throw AppException.forbidden("This community is private. Members only.");
            }
        }
    }

    private boolean moderatedBy(Long viewerId, Long communityId) {
        if (communityId == null) return false;
        CommunityMember me = communityService.membership(communityId, viewerId);
        return me != null && me.isActive() && me.getRole().canModerate();
    }

    private static boolean canDelete(Long authorId, Long communityId, Long viewerId, boolean staff, boolean communityModerator) {
        return Objects.equals(authorId, viewerId) || staff || (communityId != null && communityModerator);
    }

    private Set<Long> blockedAmong(Long viewerId, Collection<Long> userIds) {
        Set<Long> blocked = new HashSet<>();
        for (Long id : userIds) {
            if (!Objects.equals(id, viewerId) && blockRepository.existsBetween(viewerId, id)) blocked.add(id);
        }
        return blocked;
    }

    private CommentDto toDto(PostComment c, Map<Long, UserSummaryDto> people, Long viewerId, boolean staff,
                             boolean moderator, Post post, Set<Long> blocked) {
        boolean hidden = c.isDeleted() || blocked.contains(c.getAuthorId());
        boolean mine = Objects.equals(c.getAuthorId(), viewerId);
        return new CommentDto(c.getId(), c.getPostId(), c.getParentId(),
                hidden ? null : people.get(c.getAuthorId()), hidden ? null : c.getBody(), c.isDeleted(),
                mine && !c.isDeleted(), !c.isDeleted() && canDelete(c.getAuthorId(), post.getCommunityId(), viewerId, staff, moderator),
                c.getCreatedAt(), c.getEditedAt());
    }

    /** Builds DTOs for a page of posts with one query each for authors, communities, votes and memberships. */
    private List<PostDto> toDtos(List<Post> posts, Long viewerId, boolean staff) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Set<Long> communityIds = posts.stream().map(Post::getCommunityId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserSummaryDto> authors = userSummaries.of(posts.stream().map(Post::getAuthorId).collect(Collectors.toSet()));
        Map<Long, Community> communities = communityRepository.findAllById(communityIds).stream()
                .collect(Collectors.toMap(Community::getId, Function.identity()));
        Map<Long, Short> votes = voteRepository.findForUserIn(viewerId, postIds).stream()
                .collect(Collectors.toMap(v -> v.getId().getPostId(), PostVote::getValue));
        Set<Long> moderating = communityIds.isEmpty() ? Set.of() : memberRepository.findForUserIn(viewerId, communityIds).stream()
                .filter(m -> m.isActive() && m.getRole().canModerate())
                .map(m -> m.getId().getCommunityId()).collect(Collectors.toSet());

        return posts.stream().map(p -> {
            Community c = p.getCommunityId() == null ? null : communities.get(p.getCommunityId());
            boolean mine = Objects.equals(p.getAuthorId(), viewerId);
            return new PostDto(p.getId(), authors.get(p.getAuthorId()),
                    c == null ? null : new PostDto.CommunityRef(c.getSlug(), c.getName()),
                    p.getTitle(), p.getBody(), mediaUrls.url(p.getImageKey()), mediaUrls.url(p.getVideoKey()),
                    p.getScore(), votes.getOrDefault(p.getId(), (short) 0), p.getCommentCount(),
                    mine, canDelete(p.getAuthorId(), p.getCommunityId(), viewerId, staff,
                            p.getCommunityId() != null && moderating.contains(p.getCommunityId())),
                    p.getCreatedAt(), p.getEditedAt());
        }).toList();
    }

    private static int parsePage(String cursor) {
        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException e) {
            throw AppException.badRequest("Invalid cursor");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
