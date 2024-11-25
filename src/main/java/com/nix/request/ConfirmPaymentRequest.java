package com.nix.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentRequest {
	 private String paymentIntentId;
     private Integer userId;
     private Long creditPackageId;
}
