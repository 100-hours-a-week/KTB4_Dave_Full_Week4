package com.example.community.domain.user;

import java.time.Instant;


public record SignInfoDTO (
        long userNum,
        String email,
        String password,
        Instant deletedAt,
        Instant lastLogin
){

}
