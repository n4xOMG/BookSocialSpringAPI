package com.nix.dtos;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageAttachmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private Boolean isMild = false;
}
