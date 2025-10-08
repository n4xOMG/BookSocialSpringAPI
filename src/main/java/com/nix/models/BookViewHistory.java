package com.nix.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookViewHistory {
    @Id
    @UuidGenerator
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
    
    private LocalDateTime viewDate;
    private long dailyViewCount;
    
    public BookViewHistory(Book book, LocalDateTime viewDate, long dailyViewCount) {
        this.book = book;
        this.viewDate = viewDate;
        this.dailyViewCount = dailyViewCount;
    }
}