package org.church.steward.infrastructure.config;

/**
 * 설정 파일에 사용자별로 저장하는 DB 접근 키. 값은 모두 사용자의 비밀번호로 암호화된(또는 그 암호화에 쓰인) Base64 문자열이다.
 *
 * @param userId       사용자 ID
 * @param filePassword 암호화된 DB 파일 비밀번호
 * @param userPassword 암호화된 DB 사용자 비밀번호
 * @param salt         사용자 비밀번호에서 키를 유도할 때 쓴 솔트
 */
public record EncryptedUserKey(String userId, String filePassword, String userPassword, String salt) {
}
