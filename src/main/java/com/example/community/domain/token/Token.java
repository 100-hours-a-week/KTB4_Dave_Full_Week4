package com.example.community.domain.token;

import java.util.Date;
import java.util.UUID;

public record Token (
        long userNum,
        String type,// Access, Refresh
        String role,
        UUID uuid,
        Date exp
){

}
