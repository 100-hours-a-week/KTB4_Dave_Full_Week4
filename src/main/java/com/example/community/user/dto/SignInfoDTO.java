package com.example.community.user.dto;

import com.example.community.user.entity.SignInfo;

import java.time.Instant;


public record SignInfoDTO (
        long userNum,
        String email,
        String password,
        Instant deletedAt,
        Instant lastLogin
){
    public static SignInfoDTO of(UserDTO userDTO){
        return new SignInfoDTO(
                userDTO.getUserNum(),
                userDTO.getEmail(),
                userDTO.getPassword(),
                userDTO.getDeletedAt(),
                null);
    }

    public static SignInfoDTO of(SignInfo signInfo){
        return new SignInfoDTO(
                signInfo.getUserNum(),
                signInfo.getEmail(),
                signInfo.getPassword(),
                signInfo.getDeletedAt(),
                signInfo.getLastLogin()
        );
    }
}
