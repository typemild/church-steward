-- 프로그램 전체에 하나뿐인 설정(사용자별 설정이 아니다)
-- 행은 처음 저장할 때 만든다. 행이 없으면(기존 데이터, 과거 버전의 백업) 기본값을 쓴다.
CREATE TABLE PROGRAM_SETTING (
    ID                 BIGINT      NOT NULL,
    WEEKLY_CLOSING_DAY VARCHAR(10) NOT NULL,
    CONSTRAINT PK_PROGRAM_SETTING PRIMARY KEY (ID)
);

-- 예산의 비고. 기존 예산과 과거 버전의 백업은 비어 있다.
ALTER TABLE BUDGET ADD COLUMN REMARKS VARCHAR(100);

-- 비워 둘 수 있는 글자 칼럼의 "값 없음"을 null로 통일한다. 과거 버전은 입력 경로에 따라 빈 문자열로도 저장했다.
-- 공백만 있는 값도 함께 정리한다(TRIM은 반각 공백만 지우므로 전각 공백 등은 남는다. 이후 저장할 때 서비스가 정리한다).
UPDATE FINANCIAL_RECORD SET DESCRIPTION = NULL WHERE TRIM(DESCRIPTION) = '';
UPDATE FINANCIAL_RECORD SET REMARKS = NULL WHERE TRIM(REMARKS) = '';
UPDATE CONTACT SET REMARKS = NULL WHERE TRIM(REMARKS) = '';
UPDATE USERS SET REMARKS = NULL WHERE TRIM(REMARKS) = '';
