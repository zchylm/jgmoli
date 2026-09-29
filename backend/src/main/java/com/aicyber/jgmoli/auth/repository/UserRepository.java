package com.aicyber.jgmoli.auth.repository;

import com.aicyber.jgmoli.auth.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNum) -> new User(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("customer_reference"),
            resultSet.getString("email"),
            resultSet.getString("password_hash"),
            resultSet.getString("display_name"),
            resultSet.getString("role"),
            resultSet.getString("status"),
            resultSet.getObject("email_verified_at", OffsetDateTime.class),
            resultSet.getInt("auth_version"),
            resultSet.getObject("created_at", OffsetDateTime.class),
            resultSet.getObject("updated_at", OffsetDateTime.class)
    );

    private static final String SELECT_USER = """
            SELECT id, customer_reference, email, password_hash, display_name, role, status,
                   email_verified_at, auth_version, created_at, updated_at
            FROM users
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(SELECT_USER + " WHERE email = ?", USER_ROW_MAPPER, email)
                .stream()
                .findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query(SELECT_USER + " WHERE id = ?", USER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public User create(
            UUID id,
            String customerReference,
            String email,
            String passwordHash,
            String displayName,
            OffsetDateTime emailVerifiedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, customer_reference, email, password_hash, display_name, email_verified_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, customerReference, email, passwordHash, displayName, emailVerifiedAt);
        return findById(id).orElseThrow();
    }

    public void markEmailVerified(UUID id) {
        jdbcTemplate.update("""
                UPDATE users
                SET email_verified_at = COALESCE(email_verified_at, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, id);
    }

    public void updatePassword(UUID id, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?, auth_version = auth_version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, passwordHash, id);
    }
}
