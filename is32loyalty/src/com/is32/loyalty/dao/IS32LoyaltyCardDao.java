package com.is32.loyalty.dao;

import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.enums.IS32LoyaltyCardStatus;

import java.util.Date;
import java.util.List;

public interface IS32LoyaltyCardDao
{
    IS32LoyaltyCardModel findByCardNumber(String cardNumber);

    List<IS32LoyaltyCardModel> findByCustomerId(String customerId);

    List<IS32LoyaltyCardModel> findByStatus(IS32LoyaltyCardStatus status);

    List<IS32LoyaltyCardModel> findCardsByCustomerNamePattern(String namePattern);

    List<IS32LoyaltyCardModel> findActiveCardsWithTransactions(Date fromDate);

    List<IS32LoyaltyCardModel> findByEmailAddress(String email);

    List<IS32LoyaltyCardModel> findExpiredCards(Date referenceDate);

    List<IS32LoyaltyCardModel> findCardsByTierAndStatus(String tierCode, IS32LoyaltyCardStatus status);
}
