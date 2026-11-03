-- 프로그램 전체에 하나뿐인 설정(사용자별 설정이 아니다)
-- 행은 처음 저장할 때 만든다. 행이 없으면(기존 데이터, 과거 버전의 백업) 기본값을 쓴다.
CREATE TABLE PROGRAM_SETTING (
    ID                 BIGINT      NOT NULL,
    WEEKLY_CLOSING_DAY VARCHAR(10) NOT NULL,
    CONSTRAINT PK_PROGRAM_SETTING PRIMARY KEY (ID)
);

-- 예산의 비고. 기존 예산과 과거 버전의 백업은 비어 있다.
ALTER TABLE BUDGET ADD COLUMN REMARKS VARCHAR(100);
