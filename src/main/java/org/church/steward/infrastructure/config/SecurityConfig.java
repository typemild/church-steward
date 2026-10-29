package org.church.steward.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 관련 설정을 담당하는 클래스다.
 */
@Configuration
public class SecurityConfig {

    /**
     * Argon2id 알고리즘을 사용하는 PasswordEncoder를 Bean으로 등록한다.
     *
     * @return Argon2id로 비밀번호를 해싱하는 PasswordEncoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        /*
         * 기본적으로 보안성이 높은 파라미터를 설정한다. 인자는 Argon2PasswordEncoder(saltLength, hashLength,
         * parallelism, memory, iterations) 순이다.
         */
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}
