package com.nix.models;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private Boolean isMild = false;
}
