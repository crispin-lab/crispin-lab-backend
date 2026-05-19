CREATE TABLE users (
    id BIGINT NOT NULL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    handle VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
CREATE UNIQUE INDEX users_email_uidx ON users (email);
CREATE UNIQUE INDEX users_handle_uidx ON users (handle);

CREATE TABLE user_credentials (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NULL,
    oauth_provider VARCHAR(20) NULL,
    oauth_subject_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX user_credentials_user_id_idx ON user_credentials (user_id);
CREATE UNIQUE INDEX user_credentials_oauth_provider_subject_uidx
    ON user_credentials (oauth_provider, oauth_subject_id);
-- partial unique 의 'PASSWORD' 리터럴은 ExposedUserCredentialRepository.TYPE_PASSWORD 와 동일해야 한다.
-- 마이그레이션은 forward-only 라 코드 상수에서 import 할 수 없으니 두 곳을 같이 갱신할 것.
CREATE UNIQUE INDEX user_credentials_user_id_password_uidx
    ON user_credentials (user_id) WHERE type = 'PASSWORD';
