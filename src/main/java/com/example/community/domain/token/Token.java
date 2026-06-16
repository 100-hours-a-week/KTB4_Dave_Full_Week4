package com.example.community.domain.token;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@RequiredArgsConstructor
public class Token {
    private long userNum;
    private String token;
}
