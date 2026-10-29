package org.church.steward.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.church.steward.infrastructure.config.AppSettingManager;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 프로그램 버전을 조회하고 비교한다. Spring 컨텍스트가 없을 때도 쓸 수 있다.
 */
public class ProgramVersion {

    private static final String VERSION_PROPERTY = "program.info.version";
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final Resource settingFile;

    /**
     * 클래스패스의 application.yml에서 버전을 읽는다.
     * <p>
     * jar 안에 포함된 읽기 전용 파일을 읽는다. 프로그램 폴더의 application.yml과는 다른 파일이며, 그 파일에는 버전이 없다.
     * (프로그램 폴더의 application.yml : {@link AppSettingManager}가 관리하는 사용자 설정)
     */
    public ProgramVersion() {
        this(new ClassPathResource("application.yml"));
    }

    /**
     * 지정한 YAML 설정 파일에서 버전을 읽는다.
     */
    public ProgramVersion(Resource settingFile) {
        this.settingFile = settingFile;
    }

    /**
     * 실행 중인 프로그램의 버전. 설정 파일의 program.info.version에 빌드할 때 기록된 값이며, 없으면 빈 문자열이다.
     */
    public String current() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(settingFile);
        Properties properties = factory.getObject();
        return properties == null ? "" : properties.getProperty(VERSION_PROPERTY, "");
    }

    /**
     * 두 버전을 숫자 부분끼리 앞에서부터 비교한다. "-SNAPSHOT" 같은 접미어는 무시하고, 없는 자리는 0으로 본다.
     *
     * @return version이 other보다 새 버전이면 true
     */
    public static boolean isNewer(String version, String other) {
        int[] numbers = parse(version);
        int[] otherNumbers = parse(other);
        for (int i = 0; i < Math.max(numbers.length, otherNumbers.length); i++) {
            int number = i < numbers.length ? numbers[i] : 0;
            int otherNumber = i < otherNumbers.length ? otherNumbers[i] : 0;
            if (number != otherNumber) {
                return number > otherNumber;
            }
        }
        return false;
    }

    /**
     * "1.2.3-SNAPSHOT"에서 접미어를 뺀 숫자들을 꺼낸다.
     */
    private static int[] parse(String version) {
        String numericPart = version == null ? "" : version.split("-", 2)[0];
        Matcher matcher = NUMBER.matcher(numericPart);
        return matcher.results()
                .mapToInt(result -> Integer.parseInt(result.group()))
                .toArray();
    }

}
