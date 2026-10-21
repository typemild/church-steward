package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppSettingManagerTest {

    @TempDir
    Path tempDir;

    private Path yamlFile;
    private AppSettingManager appSettingManager;

    @BeforeEach
    void setUp() throws IOException {
        yamlFile = tempDir.resolve("application-test.yml");
        appSettingManager = new AppSettingManager(yamlFile);
    }

    @Nested
    @DisplayName("get 테스트")
    class GetTest {
        @Test
        @DisplayName("get_existingKey_returnsValue_logic: 기존 키에 대한 값을 정확히 조회한다.")
        void get_existingKey_returnsValue() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    server:
                      host: localhost
                      port: 8080
                    """);

            // When
            String host = appSettingManager.get("server.host");
            String port = appSettingManager.get("server.port");

            // Then
            assertThat(host).isEqualTo("localhost");
            assertThat(port).isEqualTo("8080");
        }

        @Test
        @DisplayName("get_nonExistingKey_returnsNull: 존재하지 않는 키를 조회하면 null을 반환한다.")
        void get_nonExistingKey_returnsNull() {
            // Given

            // When
            String value = appSettingManager.get("non.existent");

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("get_nullValue_returnsNull: YAML 값이 null인 경우 null을 반환한다.")
        void get_nullValue_returnsNull() throws IOException {
            // Given
            Files.writeString(yamlFile, "key: null");

            // When
            String value = appSettingManager.get("key");

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("get_nonScalarValue_throwsException: 값이 맵이나 리스트인 경우 예외를 발생시킨다.")
        void get_nonScalarValue_throwsException() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    server:
                      port: 8080
                    """);

            // When & Then
            assertThatThrownBy(() -> appSettingManager.get("server"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("스칼라 값이 아닙니다");
        }
    }

    @Nested
    @DisplayName("set 테스트")
    class SetTest {
        @Test
        @DisplayName("set_newKey_createsKey_logic: 새로운 키를 설정하면 값이 정상적으로 조회된다.")
        void set_newKey_createsKey_logic() {
            // Given

            // When
            appSettingManager.set("app.name", "Steward");

            // Then
            assertThat(appSettingManager.get("app.name")).isEqualTo("Steward");
        }

        @Test
        @DisplayName("set_newKey_createsKey_structure: 새로운 키 설정 시 계층 구조가 올바르게 생성된다.")
        void set_newKey_createsKey_structure() throws IOException {
            // Given

            // When
            appSettingManager.set("app.name", "Steward");

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualToNormalizingNewlines("""
                    app:
                      name: Steward
                    """.trim());
        }

        @Test
        @DisplayName("set_nestedKey_createsIntermediateMaps_logic: 계층 구조의 키를 설정하면 값이 정상적으로 조회된다.")
        void set_nestedKey_createsIntermediateMaps_logic() {
            // Given

            // When
            appSettingManager.set("a.b.c", "value");

            // Then
            assertThat(appSettingManager.get("a.b.c")).isEqualTo("value");
        }

        @Test
        @DisplayName("set_nestedKey_createsIntermediateMaps_structure: 계층 구조의 키 설정 시 맵들이 올바르게 생성된다.")
        void set_nestedKey_createsIntermediateMaps_structure() throws IOException {
            // Given

            // When
            appSettingManager.set("a.b.c", "value");

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualToNormalizingNewlines("""
                    a:
                      b:
                        c: value
                    """.trim());
        }

        @Test
        @DisplayName("set_existingKey_updatesValue_logic: 기존 키의 값을 수정하면 변경된 값이 조회된다.")
        void set_existingKey_updatesValue_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, "key: old");

            // When
            appSettingManager.set("key", "new");

            // Then
            assertThat(appSettingManager.get("key")).isEqualTo("new");
        }

        @Test
        @DisplayName("set_existingKey_updatesValue_structure: 기존 키 수정 시 값이 올바르게 교체된다.")
        void set_existingKey_updatesValue_structure() throws IOException {
            // Given
            Files.writeString(yamlFile, "key: old");

            // When
            appSettingManager.set("key", "new");

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualTo("key: new");
        }

        @Test
        @DisplayName("set_conflictWithMap_throwsException: 중간 경로가 이미 스칼라인 경우 예외를 발생시킨다.")
        void set_conflictWithMap_throwsException() throws IOException {
            // Given
            Files.writeString(yamlFile, "a: value");

            // When & Then
            assertThatThrownBy(() -> appSettingManager.set("a.b", "value"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("경로 충돌");
        }
    }

    @Nested
    @DisplayName("remove 테스트")
    class RemoveTest {
        @Test
        @DisplayName("remove_existingKey_removesKey_logic: 기존 키를 삭제하면 값이 조회되지 않는다.")
        void remove_existingKey_removesKey_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, "key: value");

            // When
            appSettingManager.remove("key", false);

            // Then
            assertThat(appSettingManager.get("key")).isNull();
        }

        @Test
        @DisplayName("remove_existingKey_removesKey_structure: 기존 키 삭제 시 파일에서 해당 라인이 제거된다.")
        void remove_existingKey_removesKey_structure() throws IOException {
            // Given
            Files.writeString(yamlFile, "key: value");

            // When
            appSettingManager.remove("key", false);

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("remove_nonExistingKey_doesNothing: 존재하지 않는 키를 삭제하려고 해도 예외가 발생하지 않는다.")
        void remove_nonExistingKey_doesNothing() {
            // Given

            // When & Then
            assertThatCode(() -> appSettingManager.remove("non.existent", false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("remove_mapKeyWithoutRecursive_throwsException: 하위 항목이 있는 맵을 recursive=false로 삭제하려 하면 예외를 발생시킨다.")
        void remove_mapKeyWithoutRecursive_throwsException() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    server:
                      port: 8080
                    """);

            // When & Then
            assertThatThrownBy(() -> appSettingManager.remove("server", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("하위 항목이 있는 키입니다");
        }

        @Test
        @DisplayName("remove_mapKeyWithRecursive_removesMap_logic: recursive=true로 삭제하면 하위 항목까지 조회되지 않는다.")
        void remove_mapKeyWithRecursive_removesMap_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    server:
                      port: 8080
                    """);

            // When
            appSettingManager.remove("server", true);

            // Then
            assertThat(appSettingManager.get("server")).isNull();
        }

        @Test
        @DisplayName("remove_mapKeyWithRecursive_removesMap_structure: recursive=true로 삭제하면 하위 트리가 통째로 제거된다.")
        void remove_mapKeyWithRecursive_removesMap_structure() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    server:
                      port: 8080
                    """);

            // When
            appSettingManager.remove("server", true);

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEmpty();
        }
    }

    @Nested
    @DisplayName("update 테스트")
    class UpdateTest {
        @Test
        @DisplayName("update_getWithinEditor_returnsCurrentValue_logic: update() 콜백 내에서 editor.get()을 통해 변경 전의 값을 조회할 수 있어야 한다.")
        void update_getWithinEditor_returnsCurrentValue_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, "key1: val1");

            // When & Then
            appSettingManager.update(editor -> {
                String value = editor.get("key1");
                assertThat(value).isEqualTo("val1");
            });
        }

        @Test
        @DisplayName("update_setNestedKey_createsIntermediateMaps_logic: update() 콜백 내에서 계층 구조의 키를 설정하면 값이 정상적으로 조회된다.")
        void update_setNestedKey_createsIntermediateMaps_logic() {
            // Given

            // When
            appSettingManager.update(editor -> {
                editor.set("a.b.c", "value");
            });

            // Then
            assertThat(appSettingManager.get("a.b.c")).isEqualTo("value");
        }

        @Test
        @DisplayName("update_setNestedKey_createsIntermediateMaps_structure: update() 콜백 내에서 계층 구조의 키를 설정하면 맵이 올바르게 생성된다.")
        void update_setNestedKey_createsIntermediateMaps_structure() throws IOException {
            // Given

            // When
            appSettingManager.update(editor -> {
                editor.set("a.b.c", "value");
            });

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualToNormalizingNewlines("""
                    a:
                      b:
                        c: value
                    """.trim());
        }

        @Test
        @DisplayName("update_removeNestedKey_removesSpecificKey_logic: update() 콜백 내에서 특정 계층의 키를 삭제하면 값이 조회되지 않는다.")
        void update_removeNestedKey_removesSpecificKey_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    a:
                      b: value
                      c: other
                    """);

            // When
            appSettingManager.update(editor -> {
                editor.remove("a.b", false);
            });

            // Then
            assertThat(appSettingManager.get("a.b")).isNull();
            assertThat(appSettingManager.get("a.c")).isEqualTo("other");
        }

        @Test
        @DisplayName("update_removeNestedKey_removesSpecificKey_structure: update() 콜백 내에서 특정 계층의 키를 삭제하면 구조가 유지된다.")
        void update_removeNestedKey_removesSpecificKey_structure() throws IOException {
            // Given
            Files.writeString(yamlFile, """
                    a:
                      b: value
                      c: other
                    """);

            // When
            appSettingManager.update(editor -> {
                editor.remove("a.b", false);
            });

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualToNormalizingNewlines("""
                    a:
                      c: other
                    """.trim());
        }

        @Test
        @DisplayName("update_multipleChanges_savesAll_logic: 하나의 update 트랜잭션 내에서 여러 변경 사항이 논리적으로 적용된다.")
        void update_multipleChanges_savesAll_logic() throws IOException {
            // Given
            Files.writeString(yamlFile, "key1: val1");

            // When
            appSettingManager.update(editor -> {
                editor.set("key1", "val2");
                editor.set("key2", "val3");
                editor.remove("key1", false);
            });

            // Then
            assertThat(appSettingManager.get("key1")).isNull();
            assertThat(appSettingManager.get("key2")).isEqualTo("val3");
        }

        @Test
        @DisplayName("update_multipleChanges_savesAll_structure: 하나의 update 트랜잭션 내에서 여러 변경 사항이 구조적으로 적용된다.")
        void update_multipleChanges_savesAll_structure() throws IOException {
            // Given
            Files.writeString(yamlFile, "key1: val1");

            // When
            appSettingManager.update(editor -> {
                editor.set("key1", "val2");
                editor.set("key2", "val3");
                editor.remove("key1", false);
            });

            // Then
            String content = Files.readString(yamlFile).trim();
            assertThat(content).isEqualTo("key2: val3");
        }

        @Test
        @DisplayName("update_errorDuringAction_doesNotSave: 콜백 도중 예외가 발생하면 변경 사항이 저장되지 않는다.")
        void update_errorDuringAction_doesNotSave() throws IOException {
            // Given
            Files.writeString(yamlFile, "key1: val1");

            // When
            assertThatThrownBy(() -> appSettingManager.update(editor -> {
                editor.set("key1", "val2");
                throw new RuntimeException("Error!");
            })).isInstanceOf(RuntimeException.class).hasMessage("Error!");

            // Then
            assertThat(appSettingManager.get("key1")).isEqualTo("val1");
        }
    }
}
