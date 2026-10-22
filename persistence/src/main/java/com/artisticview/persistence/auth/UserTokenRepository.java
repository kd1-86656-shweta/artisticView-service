package com.artisticview.persistence.auth;

import com.artisticview.common.enums.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, UserTokenType type);
}
