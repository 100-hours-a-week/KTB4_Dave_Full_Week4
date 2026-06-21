package com.example.community.repository.user;

import com.example.community.domain.user.SignInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignInfoJpaRepository extends JpaRepository<SignInfo, Long> {
    Optional<SignInfo> findByEmail(String email);
    Optional<SignInfo> findByUserNum(Long userNum);
    boolean existsByEmail(String email);

}
