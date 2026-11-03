-- 프로그램 전체에 하나뿐인 설정(사용자별 설정이 아니다)
-- 행은 처음 저장할 때 만든다. 행이 없으면(기존 데이터, 과거 버전의 백업) 기본값을 쓴다.
CREATE TABLE PROGRAM_SETTING (
    ID                 BIGINT      NOT NULL,
    WEEKLY_CLOSING_DAY VARCHAR(10) NOT NULL,
    CONSTRAINT PK_PROGRAM_SETTING PRIMARY KEY (ID)
);

-- 보고서 양식(프로그램 전체 설정이며 사용자별 서식인 REPORT_SETTING과 따로 둔다)
-- 행은 처음 저장할 때 만든다. 행이 없으면(기존 데이터, 과거 버전의 백업) 기본 양식을 쓴다.
-- 결재란. 직함은 칸 순서대로이며 칸 수보다 뒤의 직함과 빈 직함은 NULL이다.
CREATE TABLE REPORT_APPROVAL (
    REPORT_TYPE VARCHAR(30) NOT NULL,
    VISIBLE     BOOLEAN     NOT NULL,
    CELL_COUNT  INTEGER     NOT NULL,
    TITLE1      VARCHAR(7),
    TITLE2      VARCHAR(7),
    TITLE3      VARCHAR(7),
    TITLE4      VARCHAR(7),
    TITLE5      VARCHAR(7),
    CONSTRAINT PK_REPORT_APPROVAL PRIMARY KEY (REPORT_TYPE)
);

-- 표의 칸 순서와 숨김. 표마다 그 표의 모든 칸을 한 행씩 둔다.
CREATE TABLE REPORT_TABLE_COLUMN (
    TABLE_TYPE  VARCHAR(30) NOT NULL,
    COLUMN_TYPE VARCHAR(20) NOT NULL,
    SORT_ORDER  INTEGER     NOT NULL,
    HIDDEN      BOOLEAN     NOT NULL,
    CONSTRAINT PK_REPORT_TABLE_COLUMN PRIMARY KEY (TABLE_TYPE, COLUMN_TYPE)
);

-- 표 단위로 정하는 것(현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부)
CREATE TABLE REPORT_TABLE_OPTION (
    TABLE_TYPE       VARCHAR(30) NOT NULL,
    RATE_BAR_VISIBLE BOOLEAN     NOT NULL,
    CONSTRAINT PK_REPORT_TABLE_OPTION PRIMARY KEY (TABLE_TYPE)
);

-- 예산의 비고. 기존 예산과 과거 버전의 백업은 비어 있다.
ALTER TABLE BUDGET ADD COLUMN REMARKS VARCHAR(100);

-- 비워 둘 수 있는 글자 칼럼의 "값 없음"을 null로 통일한다. 과거 버전은 입력 경로에 따라 빈 문자열로도 저장했다.
-- 공백만 있는 값도 함께 정리한다(TRIM은 반각 공백만 지우므로 전각 공백 등은 남는다. 이후 저장할 때 서비스가 정리한다).
UPDATE FINANCIAL_RECORD SET DESCRIPTION = NULL WHERE TRIM(DESCRIPTION) = '';
UPDATE FINANCIAL_RECORD SET REMARKS = NULL WHERE TRIM(REMARKS) = '';
UPDATE CONTACT SET REMARKS = NULL WHERE TRIM(REMARKS) = '';
UPDATE USERS SET REMARKS = NULL WHERE TRIM(REMARKS) = '';

-- 지출의 내용은 필수다(수입은 비워 둘 수 있어 NOT NULL을 걸지 않고 서비스가 검증한다).
-- 내용이 비어 있던 지출은 예산명으로 채운다. 내역의 예산은 말단 예산이므로 전체 경로가 아니라 그 예산의 이름 하나다.
-- 위에서 빈 값을 NULL로 정리한 뒤에 실행해야 한다.
UPDATE FINANCIAL_RECORD R
SET DESCRIPTION = (SELECT B.NAME FROM BUDGET B WHERE B.ID = R.BUDGET_ID)
WHERE R.TYPE = 'EXPENSE' AND R.DESCRIPTION IS NULL;
