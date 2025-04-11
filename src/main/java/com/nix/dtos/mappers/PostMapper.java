package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.PostDTO;
import com.nix.models.Post;
import com.nix.models.User;

@Component
public class PostMapper implements Mapper<Post, PostDTO> {
    UserSummaryMapper userSummaryMapper = new UserSummaryMapper();
    CommentMapper commentMapper = new CommentMapper();

    @Override
    public PostDTO mapToDTO(Post p) {
        PostDTO postDTO = new PostDTO();
        if (p.getId() != null) {
            postDTO.setId(p.getId());
        }
        postDTO.setImages(p.getImages());
        postDTO.setContent(p.getContent());
        postDTO.setComments(commentMapper.mapToDTOs(p.getComments()));
        postDTO.setLikes(p.getLikes());
        postDTO.setTimestamp(p.getTimestamp());
        postDTO.setUser(userSummaryMapper.mapToDTO(p.getUser()));
        postDTO.setLikedByCurrentUser(false); // Default to false
        if (p.getSharedPost() != null) {
            postDTO.setSharedPostId(p.getSharedPost().getId());
            postDTO.setSharedPostUser(userSummaryMapper.mapToDTO(p.getSharedPost().getUser()));
            postDTO.setSharedPostContent(p.getSharedPost().getContent());
            postDTO.setSharePostImages(p.getSharedPost().getImages());
        }
        return postDTO;
    }

    @Override
    public List<PostDTO> mapToDTOs(List<Post> posts) {
        return posts.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    
    // Add these specialized methods without changing the interface
    public PostDTO mapToDTO(Post p, User currentUser) {
        PostDTO postDTO = mapToDTO(p); // Reuse existing method
        
        // Set liked status only if we have a current user
        if (currentUser != null) {
            postDTO.setLikedByCurrentUser(p.getLikedUsers().contains(currentUser));
        }
        
        return postDTO;
    }
    
    public List<PostDTO> mapToDTOs(List<Post> posts, User currentUser) {
        return posts.stream()
                .map(post -> mapToDTO(post, currentUser))
                .collect(Collectors.toList());
    }
}