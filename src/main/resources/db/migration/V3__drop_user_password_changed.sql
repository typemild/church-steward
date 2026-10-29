-- 비밀번호 변경 여부는 이번 수정에서 비밀번호를 바꿨는지를 활동 기록에 남기기 위한 값이므로 저장하지 않는다.
-- 엔터티에서 @Transient가 누락되어 칼럼으로 저장되었고, 그 때문에 두 번째 변경부터 활동 기록에 남지 않았다.
ALTER TABLE USERS DROP COLUMN IS_PASSWORD_CHAGED;
