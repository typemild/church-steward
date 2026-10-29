package org.church.steward.infrastructure.config;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * logback.xml의 로그 파일 경로({@code LOG_FILE})를 프로그램 폴더 기준으로 정한다. Logback은
 * {@code main}보다 먼저(로거를 가진 클래스가 초기화될 때) 설정을 읽으므로 시스템 속성으로 넘길 수 없어서 쓴다.
 */
public class LogFilePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        // 이 값은 교체 파일의 이름 패턴에도 들어가는데, Logback은 패턴의 역슬래시를 이스케이프 문자로 해석할 수 있으므로
        // Windows 경로도 슬래시로 적는다(Java는 Windows에서도 슬래시 경로를 받는다).
        return AppHome.logFile().toString().replace('\\', '/');
    }
}
