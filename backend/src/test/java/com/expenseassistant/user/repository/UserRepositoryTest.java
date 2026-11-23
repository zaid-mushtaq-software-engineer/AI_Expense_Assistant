package com.expenseassistant.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.config.JpaConfig;
import com.expenseassistant.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("dev")
class UserRepositoryTest {

    private static final String PLACEHOLDER_BCRYPT =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTU";

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        User saved = userRepository.save(new User("Test User", email, PLACEHOLDER_BCRYPT));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(userRepository.findByEmail(email)).map(User::getName).contains("Test User");
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void duplicateEmailFails() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User("First", email, PLACEHOLDER_BCRYPT));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("Second", email, PLACEHOLDER_BCRYPT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}