package com.example.community.user.repository;

import com.example.community.user.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoJpaRepository extends JpaRepository<UserInfo, Long>{
    List<UserInfo> findBySignInfo_UserNum(Long userNum);
    Optional<UserInfo> findByNickname(String nickname);
    Optional<UserInfo> findByProfileId(Long profileId);
    boolean existsByNickname(String nickname);
    List<UserInfo> findByProfileIdIn(List<Long> profileId);
}
