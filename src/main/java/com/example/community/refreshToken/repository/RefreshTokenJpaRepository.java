package com.example.community.refreshToken.repository;

import com.example.community.refreshToken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {
    boolean existsBySignInfo_UserNumAndToken(long userNum, String token);
    Optional<RefreshToken> findBySignInfo_UserNum(long userNum);
    Optional<RefreshToken> findByToken(String token);
}
