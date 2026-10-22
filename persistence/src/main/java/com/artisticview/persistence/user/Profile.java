package com.artisticview.persistence.user;

import com.artisticview.common.enums.ProfileVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/** Public profile. Counter columns are read-only here; they are maintained with atomic UPDATEs. */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name")
    private String displayName;

    private String bio;

    @Column(name = "avatar_key")
    private String avatarKey;

    @Column(name = "cover_key")
    private String coverKey;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] interests = new String[0];

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> links = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Column(name = "follower_count", insertable = false, updatable = false)
    private long followerCount;

    @Column(name = "following_count", insertable = false, updatable = false)
    private long followingCount;

    public Profile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }
}
