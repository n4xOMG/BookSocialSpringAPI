package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.dtos.ChatSummaryDTO;
import com.nix.models.Message;
import com.nix.models.User;

public interface MessageRepository extends JpaRepository<Message, Long> {
	// Retrieve all messages between two users ordered by timestamp
	List<Message> findBySenderAndReceiverOrderByTimestampAsc(User sender, User receiver);

	List<Message> findByReceiver(User receiver);

	@Query("""
			    SELECT m
			    FROM Message m
			    WHERE (m.sender.id = :userId OR m.receiver.id = :userId)
			      AND m.timestamp = (
			          SELECT MAX(sub.timestamp)
			          FROM Message sub
			          WHERE (sub.sender.id = m.sender.id AND sub.receiver.id = m.receiver.id)
			             OR (sub.sender.id = m.receiver.id AND sub.receiver.id = m.sender.id)
			      )
			      AND m.id = (
			          SELECT MAX(sub2.id)
			          FROM Message sub2
			          WHERE sub2.timestamp = m.timestamp
			            AND ((sub2.sender.id = m.sender.id AND sub2.receiver.id = m.receiver.id)
			              OR (sub2.sender.id = m.receiver.id AND sub2.receiver.id = m.sender.id))
			      )
			    ORDER BY m.timestamp DESC
			""")
	List<Message> findLatestMessagesWithChats(@Param("userId") Integer userId);

}
