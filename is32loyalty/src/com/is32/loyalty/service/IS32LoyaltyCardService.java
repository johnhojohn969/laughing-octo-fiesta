package com.is32.loyalty.service;

import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.enums.IS32LoyaltyCardStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IS32LoyaltyCardService
{
    IS32LoyaltyCardModel getCardByCardNumber(String cardNumber);

    List<IS32LoyaltyCardModel> getCardsByCustomerId(String customerId);

    List<IS32LoyaltyCardModel> searchCardsByCustomerName(String namePattern);

    List<IS32LoyaltyCardModel> getActiveCardsWithRecentTransactions(Date fromDate);

    List<IS32LoyaltyCardModel> getCardsByEmail(String email);

    void suspendCard(String cardNumber);

    void activateCard(String cardNumber);

    Map<String, Object> getCardSummaryWithTransactions(String cardNumber);
}
