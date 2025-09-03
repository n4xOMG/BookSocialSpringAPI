package com.nix.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

}
