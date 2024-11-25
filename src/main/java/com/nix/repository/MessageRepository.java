package com.nix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Book;
import com.nix.models.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

}
