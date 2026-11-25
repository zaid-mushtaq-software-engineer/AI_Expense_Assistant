package com.expenseassistant.conversation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.config.JpaConfig;
import com.expenseassistant.conversation.entity.ChatMessage;
import com.expenseassistant.conversation.entity.Conversation;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.user.entity.User;
import com.expenseassistant.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("dev")
class ConversationRepositoryTest {

    private static final String PLACEHOLDER_BCRYPT =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTU";

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User(
                "User A",
                "a-" + UUID.randomUUID() + "@example.com",
                PLACEHOLDER_BCRYPT));
        userB = userRepository.save(new User(
                "User B",
                "b-" + UUID.randomUUID() + "@example.com",
                PLACEHOLDER_BCRYPT));
    }

    @Test
    void userBCannotLoadUserAConversation() {
        Conversation owned = conversationRepository.save(new Conversation(userA, "Test chat"));

        assertThat(conversationRepository.findByIdAndUserId(owned.getId(), userB.getId())).isEmpty();
        assertThat(conversationRepository.findByIdAndUserId(owned.getId(), userA.getId())).isPresent();
    }

    @Test
    void deleteConversationCascadesMessages() {
        Conversation conversation = conversationRepository.save(new Conversation(userA, "Cascade test"));
        chatMessageRepository.save(new ChatMessage(conversation, MessageRole.USER, "Hello", null));
        chatMessageRepository.save(new ChatMessage(conversation, MessageRole.ASSISTANT, "Hi there", null));

        assertThat(chatMessageRepository.countByConversationId(conversation.getId())).isEqualTo(2);

        jdbcTemplate.update("DELETE FROM conversations WHERE id = ?", conversation.getId());

        Long count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM chat_messages WHERE conversation_id = ?",
                        Long.class,
                        conversation.getId());
        assertThat(count).isZero();
    }

    @Test
    void invalidRoleRejectedByDatabase() {
        Conversation conversation = conversationRepository.save(new Conversation(userA, "Role test"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO chat_messages (conversation_id, role, content)
                        VALUES (?, 'BOT', 'bad')
                        """,
                        conversation.getId()))
                .hasMessageContaining("chk_messages_role");
    }
}