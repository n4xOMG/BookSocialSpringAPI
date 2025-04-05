package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.PurchaseDTO;
import com.nix.dtos.SalesPerUserDTO;
import com.nix.models.User;
import com.nix.service.PurchaseService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private UserService userService;

    @GetMapping("/history")
    public ResponseEntity<List<PurchaseDTO>> getPurchaseHistory(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        List<PurchaseDTO> purchaseHistory = purchaseService.getPurchaseHistoryForUser(user.getId());
        return ResponseEntity.ok(purchaseHistory);
    }

    @GetMapping("/admin/total-sales")
    public ResponseEntity<Double> getTotalSalesAmount() {
        Double totalSales = purchaseService.getTotalSalesAmount();
        return ResponseEntity.ok(totalSales);
    }

    @GetMapping("/admin/total-number")
    public ResponseEntity<Long> getTotalNumberOfPurchases() {
        Long totalPurchases = purchaseService.getTotalNumberOfPurchases();
        return ResponseEntity.ok(totalPurchases);
    }

    @GetMapping("/admin/sales-per-user")
    public ResponseEntity<List<SalesPerUserDTO>> getSalesStatisticsPerUser() {
        List<SalesPerUserDTO> salesPerUser = purchaseService.getSalesStatisticsPerUser();
        return ResponseEntity.ok(salesPerUser);
    }
}
