package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChapterDTO {
    private Integer id;
    private String chapterNum; 
    private String title;
    private String content;
    private Integer translatorId;
    private LocalDateTime uploadDate;
    private Integer bookId; 
    private List<CommentDTO> comments;
}
