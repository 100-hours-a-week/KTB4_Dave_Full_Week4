package com.example.community.resolver;

import com.example.community.domain.exception.UnAuthorizedException;
import com.example.community.domain.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
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
                                            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        SignUser signUser = parameter.getParameterAnnotation(SignUser.class);
        boolean required = signUser.required();

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("HttpServletRequest를 찾을 수 없습니다.");
        }

        Long userNum = (Long) request.getAttribute("userNum");
        Long profileId = (Long) request.getAttribute("profileId");
        UserRole role = (UserRole) request.getAttribute("role");
        if ((userNum == null || role == null) && required) {
            throw new UnAuthorizedException("로그인이 필요합니다.");
        }

        if (userNum == null || role == null) {
            return null;
        }
        return new SignUserInfo(userNum, profileId, role);
    }
}
