package com.nix.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {
	private Long id;
	private UserSummaryDTO userOne;
	private UserSummaryDTO userTwo;
	private List<MessageDTO> messages;

}
