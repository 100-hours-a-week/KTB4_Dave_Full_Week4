package com.example.community.domain.user;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class CustomUserDetails  implements UserDetails {
    private final String userName;
    private final String password;
    private final UserRole role;
    private final Long userNum;
    private final Long profileId;

    public CustomUserDetails(Long userNum, Long profileId, UserRole role) {
        this.userNum = userNum;
        this.profileId = profileId;
        this.role = role;
        this.userName = null;
        this.password = null;
    }

    public CustomUserDetails(Long userNum, Long profileId, String role) {
        this.userNum = userNum;
        this.profileId = profileId;
        this.role = UserRole.valueOf(role);
        this.userName = null;
        this.password = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + getRole()));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    public String getRole() {
        return role.name();
    }
}
