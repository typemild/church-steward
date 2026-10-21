-- 테스트용 가짜 마이그레이션: 중간에 실패하는 마이그레이션을 흉내 낸다.
-- H2는 DDL을 문장마다 바로 커밋하므로 첫 문장은 반영된 채로 남는다.
ALTER TABLE CONTACT ADD COLUMN TEST_MEMO VARCHAR(20);
INSERT INTO NO_SUCH_TABLE VALUES (1);
