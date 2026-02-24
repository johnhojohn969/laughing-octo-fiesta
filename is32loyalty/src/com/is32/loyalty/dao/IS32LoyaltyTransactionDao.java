package com.is32.loyalty.dao;

import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.enums.IS32LoyaltyTransactionType;

import java.util.Date;
import java.util.List;

public interface IS32LoyaltyTransactionDao
{
    IS32LoyaltyTransactionModel findByTransactionId(String transactionId);

    List<IS32LoyaltyTransactionModel> findByCardNumber(String cardNumber);

    List<IS32LoyaltyTransactionModel> findByMerchantCode(String merchantCode);

    List<IS32LoyaltyTransactionModel> findByTransactionType(IS32LoyaltyTransactionType transactionType);

    List<IS32LoyaltyTransactionModel> findByCardNumberAndDateRange(String cardNumber, Date startDate, Date endDate);

    List<List<Object>> getTransactionSummaryByCard(String cardNumber);

    List<IS32LoyaltyTransactionModel> findByDescriptionKeyword(String keyword);

    List<IS32LoyaltyTransactionModel> findAllTransactionsForCard(String cardNumber);

    List<List<Object>> getMerchantTransactionReport(String merchantCode, Date startDate, Date endDate);

    List<IS32LoyaltyTransactionModel> findByStoreId(String storeId);

    List<IS32LoyaltyTransactionModel> findRecentTransactionsByEmail(String email);
}
