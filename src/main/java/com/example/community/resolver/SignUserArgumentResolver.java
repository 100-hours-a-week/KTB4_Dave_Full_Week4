package com.example.community.resolver;

import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.entity.CustomUserDetails;
import com.example.community.user.entity.UserRole;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class SignUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SignUser.class)
                && parameter.getParameterType().equals(SignUserInfo.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getPrincipal().equals("anonymousUser")){
            return null;
        }
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        Long userNum = user.getUserNum();
        Long profileId = user.getProfileId();
        UserRole role = UserRole.valueOf(user.getRole());
        if (userNum == null || profileId == null ) {
            throw new UnAuthorizedException("로그인이 필요합니다.");
        }
        return new SignUserInfo(userNum, profileId, role);
    }
}
