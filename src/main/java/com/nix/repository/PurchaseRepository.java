package com.nix.repository;

import java.math.BigDecimal;
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
	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM Purchase p WHERE p.status = 'COMPLETED'")
	BigDecimal getTotalRevenue();

	@Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED'")
	Long getTotalCompletedPurchases();

	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate")
	BigDecimal getRevenueFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate")
	Long getPurchasesFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT COALESCE(AVG(p.amount), 0) FROM Purchase p WHERE p.status = 'COMPLETED'")
	BigDecimal getAverageOrderValue();

	@Query("SELECT p.paymentProvider, COUNT(p) FROM Purchase p WHERE p.status = 'COMPLETED' GROUP BY p.paymentProvider")
	List<Object[]> getPaymentProviderStats();

	@Query("SELECT COALESCE(SUM(p.amount), 0), COALESCE(SUM(p.creditsPurchased), 0) FROM Purchase p WHERE p.status = :status")
	Object[] getTotalsByStatus(@Param("status") PaymentStatus status);

	@Query("SELECT CAST(p.purchaseDate AS LocalDate), SUM(p.amount) FROM Purchase p WHERE p.status = 'COMPLETED' AND p.purchaseDate >= :startDate GROUP BY CAST(p.purchaseDate AS LocalDate) ORDER BY CAST(p.purchaseDate AS LocalDate)")
	List<Object[]> getDailyRevenue(@Param("startDate") LocalDateTime startDate);

	List<Purchase> findByStatusAndPaymentProvider(PaymentStatus status, PaymentProvider provider);
}
