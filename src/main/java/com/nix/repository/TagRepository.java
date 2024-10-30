package com.nix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer>{

}
