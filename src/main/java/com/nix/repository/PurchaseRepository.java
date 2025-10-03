package com.nix.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.enums.PaymentProvider;
import com.nix.enums.PaymentStatus;
import com.nix.models.Purchase;
import com.nix.models.User;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

	boolean existsByPaymentIntentId(String paymentIntentId);

	Purchase findByPaymentIntentId(String paymentIntentId);

	List<Purchase> findByUser(User user);

	// Analytics queries
	@Query("SELECT SUM(p.amount) FROM Purchase p WHERE p.status = 'COMPLETED'")
	Double getTotalRevenue();

	@Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED'")
	Long getTotalCompletedPurchases();

	@Query("SELECT SUM(p.amount) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate")
	Double getRevenueFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate")
	Long getPurchasesFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT AVG(p.amount) FROM Purchase p WHERE p.status = 'COMPLETED'")
	Double getAverageOrderValue();

	@Query("SELECT p.paymentProvider, COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED' GROUP BY p.paymentProvider")
	List<Object[]> getPaymentProviderStats();

	@Query("SELECT DATE(p.purchaseDate), SUM(p.amount) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate GROUP BY DATE(p.purchaseDate) ORDER BY DATE(p.purchaseDate)")
	List<Object[]> getDailyRevenue(@Param("startDate") LocalDateTime startDate);

	List<Purchase> findByStatusAndPaymentProvider(PaymentStatus status, PaymentProvider provider);
}
