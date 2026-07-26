package com.meetingiq.platform.repository;

import com.meetingiq.platform.domain.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Plain Spring Data CRUD for persisted RAG chat threads. */
public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

    List<ChatConversation> findByMeetingIdOrderByUpdatedAtDesc(String meetingId);
}
