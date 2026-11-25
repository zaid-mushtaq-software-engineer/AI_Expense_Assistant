package com.expenseassistant.conversation.repository;

import com.expenseassistant.conversation.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId, Pageable pageable);

    long countByConversationId(Long conversationId);

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId);

    @Query(
            value =
                    """
            SELECT m.* FROM chat_messages m
            INNER JOIN (
                SELECT conversation_id, MAX(id) AS max_id
                FROM chat_messages
                WHERE conversation_id IN (:conversationIds)
                GROUP BY conversation_id
            ) latest ON m.id = latest.max_id
            """,
            nativeQuery = true)
    List<ChatMessage> findLatestByConversationIds(@Param("conversationIds") List<Long> conversationIds);
}