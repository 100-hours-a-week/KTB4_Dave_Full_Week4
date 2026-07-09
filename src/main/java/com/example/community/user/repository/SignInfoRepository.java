package com.example.community.user.repository;

import com.example.community.user.entity.SignInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignInfoRepository extends JpaRepository<SignInfo, Long> {
    Optional<SignInfo> findByEmail(String email);
    Optional<SignInfo> findByUserNum(Long userNum);
    boolean existsByEmail(String email);

}
