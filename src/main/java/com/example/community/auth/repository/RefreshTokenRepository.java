package com.example.community.auth.repository;

import com.example.community.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @EntityGraph(attributePaths = {"signInfo"})
    Optional<RefreshToken> findByToken(String token);
    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken r where r.token = :token")
    void deleteByToken(String token);
}
