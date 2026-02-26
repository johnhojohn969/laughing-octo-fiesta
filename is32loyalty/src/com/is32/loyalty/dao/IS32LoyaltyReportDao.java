package com.is32.loyalty.dao;

import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;

import java.util.Date;
import java.util.List;

public interface IS32LoyaltyReportDao
{
    List<IS32LoyaltyCardModel> findCardsByNumericCardNumber(long cardNumber);

    List<IS32LoyaltyCardModel> findCardsByTotalPointsEarned(String minPoints);

    List<IS32LoyaltyTransactionModel> findTransactionsByMerchantId(int merchantCode);

    List<IS32LoyaltyTransactionModel> findTransactionsByAmountRange(String minAmount, String maxAmount);

    List<IS32LoyaltyTransactionModel> findTransactionsByPointsEarned(String points);

    List<IS32LoyaltyCardModel> findCardsByCustomerIdNumeric(long customerId);

    List<List<Object>> getCardBalanceReport(int cardNumber);

    List<IS32LoyaltyTransactionModel> findTransactionsByOrderCode(int orderCode);
}
