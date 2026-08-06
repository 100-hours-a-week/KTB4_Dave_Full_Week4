package com.example.community.resolver;

import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.entity.CustomUserDetails;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignUserArgumentResolverTest {

    private final SignUserArgumentResolver resolver =
            new SignUserArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SignUser가 선언된 SignUserInfo 파라미터를 지원한다")
    void supportsAnnotatedSignUserInfoParameter() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(
                "annotatedSignUserInfo",
                SignUserInfo.class
        );

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    @DisplayName("SignUser가 없는 파라미터는 지원하지 않는다")
    void doesNotSupportUnannotatedParameter() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(
                "unannotatedSignUserInfo",
                SignUserInfo.class
        );

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    @DisplayName("SignUser가 있어도 SignUserInfo 타입이 아니면 지원하지 않는다")
    void doesNotSupportDifferentParameterType() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(
                "annotatedString",
                String.class
        );

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 로그인 사용자 정보를 반환하지 않는다")
    void returnsNullWithoutAuthentication() throws NoSuchMethodException {
        Object result = resolveArgument();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("익명 사용자이면 로그인 사용자 정보를 반환하지 않는다")
    void returnsNullForAnonymousUser() throws NoSuchMethodException {
        setPrincipal("anonymousUser");

        Object result = resolveArgument();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("인증된 사용자의 식별자와 역할을 SignUserInfo로 변환한다")
    void resolvesAuthenticatedUser() throws NoSuchMethodException {
        setPrincipal(new CustomUserDetails(1L, 2L, UserRole.ADMIN));

        Object result = resolveArgument();

        assertThat(result).isEqualTo(
                new SignUserInfo(1L, 2L, UserRole.ADMIN)
        );
    }

    @ParameterizedTest(name = "userNum={0}, profileId={1}")
    @MethodSource("missingIdentifierUsers")
    @DisplayName("인증 사용자의 식별자가 누락되면 로그인이 필요한 것으로 처리한다")
    void rejectsAuthenticatedUserWithoutIdentifier(
            Long userNum,
            Long profileId
    ) throws NoSuchMethodException {
        setPrincipal(new CustomUserDetails(
                userNum,
                profileId,
                UserRole.USER
        ));

        assertThatThrownBy(this::resolveArgument)
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인이 필요합니다.");
    }

    private Object resolveArgument() throws NoSuchMethodException {
        return resolver.resolveArgument(
                methodParameter("annotatedSignUserInfo", SignUserInfo.class),
                null,
                mock(NativeWebRequest.class),
                null
        );
    }

    private void setPrincipal(Object principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MethodParameter methodParameter(
            String methodName,
            Class<?> parameterType
    ) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(
                methodName,
                parameterType
        );
        return new MethodParameter(method, 0);
    }

    private static Stream<Arguments> missingIdentifierUsers() {
        return Stream.of(
                Arguments.of(null, 2L),
                Arguments.of(1L, null)
        );
    }

    private static class TestController {
        void annotatedSignUserInfo(@SignUser SignUserInfo signUserInfo) {
        }

        void unannotatedSignUserInfo(SignUserInfo signUserInfo) {
        }

        void annotatedString(@SignUser String value) {
        }
    }
}
