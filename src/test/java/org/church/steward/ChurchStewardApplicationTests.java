package org.church.steward;

import org.church.steward.ui.view.main.MainView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring 컨텍스트 전체가 뜨는지 확인한다. 빈 연결(빠진 빈, 순환 의존, 같은 타입 빈의 중복), 트랜잭션 프록시, 설정 바인딩,
 * Flyway 마이그레이션과 {@code ddl-auto: validate}를 한 번에 확인하는 유일한 테스트다.
 *
 * 실제 실행과 다른 점은 두 가지다.
 * <ul>
 * <li>{@code ChurchStewardApplication#dataSource()}는 로그인에서 복호화한 DB 비밀번호가 있어야 만들
 * 수 있고, 그대로 쓰면 저장소 루트의
 * {@code ./data}를 가리키므로 내장 H2로 바꾼다.</li>
 * <li>{@link MainView}는 {@code JFrame}이라 headless 환경({@code SpringApplication}의
 * 기본값)에서 만들 수 없으므로 mock으로 바꾼다.
 * 나머지 화면 빈은 그대로 만들어 생성자 주입까지 확인한다.</li>
 * </ul>
 * 기동 절차({@code ChurchStewardApplication.main}의 최초 실행, 로그인, 복원)는 테스트하지
 * 않는다(docs/architecture.md의
 * "기동 절차와 데이터베이스 암호화" 참조).
 */
// application.yml의 "${version}"은 Gradle(processResources)이 빌드할 때 채운다. IDE가 빌드한 결과로 실행하면
// 채워지지 않은 채 placeholder로 남아 기동에 실패하므로, 그때만 쓰이도록 값을 넣어 둔다.
@SpringBootTest(properties = "version=test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ChurchStewardApplicationTests {

    @MockitoBean
    private MainView mainView;

    @Test
    @DisplayName("내장 데이터베이스로 바꾸면 모든 빈이 연결되어 애플리케이션 컨텍스트가 정상적으로 뜬다")
    void contextLoads_withEmbeddedDatabase_succeeds() {
        // Then: 컨텍스트가 뜨지 않으면 테스트 메서드에 들어오기 전에 실패한다.
    }

}
