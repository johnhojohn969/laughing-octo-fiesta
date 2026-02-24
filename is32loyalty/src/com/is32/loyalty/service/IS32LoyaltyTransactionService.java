package com.is32.loyalty.service;

import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.enums.IS32LoyaltyTransactionType;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IS32LoyaltyTransactionService
{
    IS32LoyaltyTransactionModel getTransactionById(String transactionId);

    List<IS32LoyaltyTransactionModel> getTransactionsForCard(String cardNumber);

    List<IS32LoyaltyTransactionModel> getTransactionsByMerchant(String merchantCode);

    Map<String, Double> getTransactionSummary(String cardNumber);

    List<IS32LoyaltyTransactionModel> searchTransactionsByDescription(String keyword);

    List<Map<String, Object>> getMerchantReport(String merchantCode, Date startDate, Date endDate);

    List<IS32LoyaltyTransactionModel> getRecentTransactionsByCustomerEmail(String email);
}
