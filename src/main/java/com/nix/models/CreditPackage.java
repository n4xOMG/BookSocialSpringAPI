package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditPackage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private int creditAmount;
	private double price;
	private boolean isActive;

	@OneToMany(mappedBy = "creditPackage")
	private List<Purchase> purchases;

	public BigDecimal calculatePricePerCredit() {
		if (price <= 0 || creditAmount <= 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(price).divide(BigDecimal.valueOf(creditAmount), 4, RoundingMode.HALF_UP);
	}

}
