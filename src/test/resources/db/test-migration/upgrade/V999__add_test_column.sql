-- 테스트용 가짜 마이그레이션: 새 버전의 프로그램이 칼럼을 추가한 상황을 흉내 낸다.
-- 실제 마이그레이션 위치(db/migration)와 섞이지 않도록 별도 위치에 두고, DatabaseManager에 위치를 넘겨서만 쓴다.
ALTER TABLE CONTACT ADD COLUMN TEST_MEMO VARCHAR(20) DEFAULT 'migrated';
