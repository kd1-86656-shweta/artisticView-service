package com.artisticview.core.community;

import com.artisticview.common.dto.CommunityDto;
import com.artisticview.common.dto.CommunityMemberDto;
import com.artisticview.common.dto.CommunityRequest;
import com.artisticview.common.dto.UserSummaryDto;
import com.artisticview.common.enums.CommunityRole;
import com.artisticview.common.enums.CommunityVisibility;
import com.artisticview.common.enums.MediaKind;
import com.artisticview.common.enums.MembershipStatus;
import com.artisticview.common.exception.AppException;
import com.artisticview.common.util.Limits;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.core.user.UserSummaries;
import com.artisticview.persistence.community.Community;
import com.artisticview.persistence.community.CommunityMember;
import com.artisticview.persistence.community.CommunityMemberId;
import com.artisticview.persistence.community.CommunityMemberRepository;
import com.artisticview.persistence.community.CommunityRepository;
import com.artisticview.persistence.social.BlockRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reddit-style communities. Anyone with COMMUNITY_CREATE can start one and becomes its OWNER. Public communities
 * can be joined by anyone; private ones by invite. Owners and moderators invite and remove members; only the
 * owner promotes moderators or deletes the community.
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final UserSummaries userSummaries;
    private final MediaUrls mediaUrls;

    // ------------------------------------------------------------------ reading

    @Transactional(readOnly = true)
    public List<CommunityDto> discover(Long viewerId, String q, Integer page, Integer size) {
        int limit = Limits.pageSize(size);
        String search = q == null || q.isBlank() ? null : q.trim();
        return toDtos(communityRepository.discover(search, limit, Math.max(0, page == null ? 0 : page) * limit), viewerId);
    }

    @Transactional(readOnly = true)
    public List<CommunityDto> joined(Long viewerId) {
        return toDtos(communityRepository.findJoined(viewerId), viewerId);
    }

    @Transactional(readOnly = true)
    public List<CommunityDto> invites(Long viewerId) {
        List<Long> ids = memberRepository.findByUserAndStatus(viewerId, MembershipStatus.INVITED).stream()
                .map(m -> m.getId().getCommunityId()).toList();
        return toDtos(communityRepository.findAllById(ids), viewerId);
    }

    @Transactional(readOnly = true)
    public CommunityDto get(String slug, Long viewerId) {
        return toDtos(List.of(load(slug)), viewerId).get(0);
    }

    /** Members list. In a private community only members can see who else is in it. */
    @Transactional(readOnly = true)
    public List<CommunityMemberDto> members(String slug, Long viewerId) {
        Community c = load(slug);
        CommunityMember me = membership(c.getId(), viewerId);
        boolean insider = me != null && me.isActive();
        if (c.getVisibility() == CommunityVisibility.PRIVATE && !insider) {
            throw AppException.forbidden("Only members can see who is in this community");
        }
        boolean moderator = insider && me.getRole().canModerate();
        List<CommunityMember> rows = memberRepository.findByCommunity(c.getId()).stream()
                .filter(m -> m.isActive() || moderator) // pending invites are visible to the people who sent them
                .toList();
        Map<Long, UserSummaryDto> people = userSummaries.of(rows.stream().map(m -> m.getId().getUserId()).toList());
        return rows.stream()
                .map(m -> new CommunityMemberDto(people.get(m.getId().getUserId()), m.getRole(), m.getStatus(), m.getCreatedAt()))
                .toList();
    }

    // ------------------------------------------------------------------ create / update / delete

    @Transactional
    public CommunityDto create(Long ownerId, CommunityRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw AppException.badRequest("Give your community a name");
        }
        String slug = request.slug() != null ? request.slug() : uniqueSlug(request.name());
        if (communityRepository.existsBySlug(slug)) {
            throw AppException.conflict("The address /c/" + slug + " is taken, try another");
        }
        Community c = new Community();
        c.setSlug(slug);
        c.setCreatorId(ownerId);
        c.setVisibility(CommunityVisibility.PUBLIC);
        apply(c, request, ownerId);
        communityRepository.save(c);
        memberRepository.save(new CommunityMember(c.getId(), ownerId, CommunityRole.OWNER, MembershipStatus.ACTIVE, null));
        communityRepository.adjustMembers(c.getId(), 1);
        return get(slug, ownerId);
    }

    @Transactional
    public CommunityDto update(String slug, Long viewerId, CommunityRequest request) {
        Community c = load(slug);
        requireModerator(c, viewerId);
        if (request.name() != null && request.name().isBlank()) {
            throw AppException.badRequest("The name cannot be empty");
        }
        apply(c, request, viewerId);
        communityRepository.save(c);
        return get(slug, viewerId);
    }

    /** Deletes the community with all its posts, comments and memberships. Owner or platform moderators only. */
    @Transactional
    public void delete(String slug, Long viewerId, boolean staff) {
        Community c = load(slug);
        CommunityMember me = membership(c.getId(), viewerId);
        if (!staff && (me == null || me.getRole() != CommunityRole.OWNER)) {
            throw AppException.forbidden("Only the owner can delete this community");
        }
        communityRepository.delete(c);
    }

    private void apply(Community c, CommunityRequest r, Long userId) {
        if (r.name() != null) c.setName(r.name().trim());
        if (r.description() != null) c.setDescription(blankToNull(r.description()));
        if (r.rules() != null) c.setRules(blankToNull(r.rules()));
        if (r.visibility() != null) c.setVisibility(r.visibility());
        if (r.coverKey() != null) {
            if (!r.coverKey().isBlank() && !r.coverKey().equals(c.getCoverKey())
                    && !MediaKind.COMMUNITY.ownedBy(r.coverKey(), userId)) {
                throw AppException.badRequest("Upload the cover image again and retry");
            }
            c.setCoverKey(blankToNull(r.coverKey()));
        }
    }

    /** "Budnix Cypher Crew!" -> "budnix-cypher-crew", adding -2, -3... if it is taken. */
    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.length() > 36) base = base.substring(0, 36).replaceAll("-+$", "");
        if (base.length() < 3) base = (base + "-community").replaceAll("^-", "");
        String slug = base;
        for (int i = 2; communityRepository.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    // ------------------------------------------------------------------ membership

    /** Join a public community, or accept an invite to any community. */
    @Transactional
    public CommunityDto join(String slug, Long viewerId) {
        Community c = load(slug);
        CommunityMember me = membership(c.getId(), viewerId);
        if (me != null && me.isActive()) {
            return get(slug, viewerId);
        }
        if (blockRepository.existsBetween(viewerId, c.getCreatorId())) {
            throw AppException.forbidden("You cannot join this community");
        }
        if (me != null) {
            me.setStatus(MembershipStatus.ACTIVE);
            memberRepository.save(me);
        } else if (c.getVisibility() == CommunityVisibility.PUBLIC) {
            memberRepository.save(new CommunityMember(c.getId(), viewerId, CommunityRole.MEMBER, MembershipStatus.ACTIVE, null));
        } else {
            throw AppException.forbidden("This community is invite-only");
        }
        communityRepository.adjustMembers(c.getId(), 1);
        return get(slug, viewerId);
    }

    /** Leave a community, or decline an invite. The owner has to delete the community instead. */
    @Transactional
    public CommunityDto leave(String slug, Long viewerId) {
        Community c = load(slug);
        CommunityMember me = membership(c.getId(), viewerId);
        if (me == null) {
            return get(slug, viewerId);
        }
        if (me.getRole() == CommunityRole.OWNER) {
            throw AppException.badRequest("The owner cannot leave. Delete the community instead.");
        }
        memberRepository.delete(me);
        if (me.isActive()) {
            communityRepository.adjustMembers(c.getId(), -1);
        }
        return get(slug, viewerId);
    }

    /** Owners and moderators add people: they get an invite and join once they accept. */
    @Transactional
    public List<CommunityMemberDto> invite(String slug, Long viewerId, String rawUsername) {
        Community c = load(slug);
        requireModerator(c, viewerId);
        String username = rawUsername.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(username).filter(User::isActive)
                .orElseThrow(() -> AppException.badRequest("No member called @" + username));
        if (blockRepository.existsBetween(viewerId, user.getId())) {
            throw AppException.badRequest("You cannot add @" + username);
        }
        CommunityMember existing = membership(c.getId(), user.getId());
        if (existing != null) {
            throw AppException.conflict(existing.isActive() ? "@" + username + " is already a member" : "@" + username + " already has an invite");
        }
        memberRepository.save(new CommunityMember(c.getId(), user.getId(), CommunityRole.MEMBER, MembershipStatus.INVITED, viewerId));
        return members(slug, viewerId);
    }

    /** Owners remove anyone but themselves; moderators remove members (not other moderators). */
    @Transactional
    public List<CommunityMemberDto> removeMember(String slug, Long viewerId, Long userId) {
        Community c = load(slug);
        CommunityMember me = requireModerator(c, viewerId);
        CommunityMember target = membership(c.getId(), userId);
        if (target == null) {
            throw AppException.notFound("Member");
        }
        if (target.getRole() == CommunityRole.OWNER) {
            throw AppException.badRequest("The owner cannot be removed");
        }
        if (target.getRole() == CommunityRole.MODERATOR && me.getRole() != CommunityRole.OWNER) {
            throw AppException.forbidden("Only the owner can remove a moderator");
        }
        memberRepository.delete(target);
        if (target.isActive()) {
            communityRepository.adjustMembers(c.getId(), -1);
        }
        return members(slug, viewerId);
    }

    /** The owner makes an active member a moderator, or a moderator a member again. */
    @Transactional
    public List<CommunityMemberDto> setRole(String slug, Long viewerId, Long userId, CommunityRole role) {
        Community c = load(slug);
        CommunityMember me = membership(c.getId(), viewerId);
        if (me == null || me.getRole() != CommunityRole.OWNER) {
            throw AppException.forbidden("Only the owner can change roles");
        }
        if (role == CommunityRole.OWNER) {
            throw AppException.badRequest("Ownership cannot be transferred here");
        }
        CommunityMember target = membership(c.getId(), userId);
        if (target == null || !target.isActive() || target.getRole() == CommunityRole.OWNER) {
            throw AppException.badRequest("Only active members can become moderators");
        }
        target.setRole(role);
        memberRepository.save(target);
        return members(slug, viewerId);
    }

    // ------------------------------------------------------------------ helpers used by posts too

    public Community load(String slug) {
        return communityRepository.findBySlug(slug.toLowerCase(Locale.ROOT)).orElseThrow(() -> AppException.notFound("Community"));
    }

    public CommunityMember membership(Long communityId, Long userId) {
        return userId == null ? null : memberRepository.findById(new CommunityMemberId(communityId, userId)).orElse(null);
    }

    private CommunityMember requireModerator(Community c, Long viewerId) {
        CommunityMember me = membership(c.getId(), viewerId);
        if (me == null || !me.isActive() || !me.getRole().canModerate()) {
            throw AppException.forbidden("Only the owner and moderators can do this");
        }
        return me;
    }

    private List<CommunityDto> toDtos(Collection<Community> communities, Long viewerId) {
        if (communities.isEmpty()) {
            return List.of();
        }
        List<Long> ids = communities.stream().map(Community::getId).toList();
        Map<Long, CommunityMember> mine = viewerId == null ? Map.of() : memberRepository.findForUserIn(viewerId, ids).stream()
                .collect(Collectors.toMap(m -> m.getId().getCommunityId(), Function.identity()));
        Set<Long> creators = new HashSet<>();
        communities.forEach(c -> creators.add(c.getCreatorId()));
        Map<Long, UserSummaryDto> people = userSummaries.of(creators);
        return communities.stream().map(c -> {
            CommunityMember m = mine.get(c.getId());
            return new CommunityDto(c.getId(), c.getSlug(), c.getName(), c.getDescription(), c.getRules(),
                    c.getVisibility(), mediaUrls.url(c.getCoverKey()), c.getMemberCount(), c.getPostCount(),
                    people.get(c.getCreatorId()), m == null ? null : m.getRole(), m == null ? null : m.getStatus(),
                    m != null && m.isActive() && m.getRole().canModerate(), c.getCreatedAt());
        }).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
