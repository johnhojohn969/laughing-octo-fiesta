package com.is32.loyalty.service.impl;

import com.is32.loyalty.dao.IS32LoyaltyCardDao;
import com.is32.loyalty.dao.IS32LoyaltyTransactionDao;
import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.enums.IS32LoyaltyCardStatus;
import com.is32.loyalty.service.IS32LoyaltyCardService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32LoyaltyCardService implements IS32LoyaltyCardService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32LoyaltyCardService.class);

    private IS32LoyaltyCardDao is32LoyaltyCardDao;
    private IS32LoyaltyTransactionDao is32LoyaltyTransactionDao;
    private ModelService modelService;

    @Override
    public IS32LoyaltyCardModel getCardByCardNumber(final String cardNumber)
    {
        return is32LoyaltyCardDao.findByCardNumber(cardNumber);
    }

    @Override
    public List<IS32LoyaltyCardModel> getCardsByCustomerId(final String customerId)
    {
        return is32LoyaltyCardDao.findByCustomerId(customerId);
    }

    @Override
    public List<IS32LoyaltyCardModel> searchCardsByCustomerName(final String namePattern)
    {
        return is32LoyaltyCardDao.findCardsByCustomerNamePattern(namePattern);
    }

    @Override
    public List<IS32LoyaltyCardModel> getActiveCardsWithRecentTransactions(final Date fromDate)
    {
        return is32LoyaltyCardDao.findActiveCardsWithTransactions(fromDate);
    }

    @Override
    public List<IS32LoyaltyCardModel> getCardsByEmail(final String email)
    {
        return is32LoyaltyCardDao.findByEmailAddress(email);
    }

    @Override
    public void suspendCard(final String cardNumber)
    {
        final IS32LoyaltyCardModel card = is32LoyaltyCardDao.findByCardNumber(cardNumber);
        card.setStatus(IS32LoyaltyCardStatus.SUSPENDED);
        modelService.save(card);
    }

    @Override
    public void activateCard(final String cardNumber)
    {
        final IS32LoyaltyCardModel card = is32LoyaltyCardDao.findByCardNumber(cardNumber);
        card.setStatus(IS32LoyaltyCardStatus.ACTIVE);
        modelService.save(card);
    }

    @Override
    public Map<String, Object> getCardSummaryWithTransactions(final String cardNumber)
    {
        final IS32LoyaltyCardModel card = is32LoyaltyCardDao.findByCardNumber(cardNumber);
        final Map<String, Object> summary = new HashMap<>();
        summary.put("card", card);
        summary.put("customerName", card.getCustomerName());
        summary.put("status", card.getStatus());
        summary.put("tierCode", card.getTierCode());

        final List<IS32LoyaltyTransactionModel> transactions = is32LoyaltyTransactionDao.findByCardNumber(cardNumber);
        summary.put("transactions", transactions);
        summary.put("transactionCount", transactions.size());

        double totalEarned = 0;
        double totalRedeemed = 0;
        for (final IS32LoyaltyTransactionModel txn : transactions)
        {
            if (txn.getPointsEarned() != null)
            {
                totalEarned += txn.getPointsEarned();
            }
            if (txn.getPointsRedeemed() != null)
            {
                totalRedeemed += txn.getPointsRedeemed();
            }
        }

        summary.put("totalPointsEarned", totalEarned);
        summary.put("totalPointsRedeemed", totalRedeemed);
        summary.put("netPoints", totalEarned - totalRedeemed);

        return summary;
    }

    public void setIs32LoyaltyCardDao(final IS32LoyaltyCardDao is32LoyaltyCardDao)
    {
        this.is32LoyaltyCardDao = is32LoyaltyCardDao;
    }

    public void setIs32LoyaltyTransactionDao(final IS32LoyaltyTransactionDao is32LoyaltyTransactionDao)
    {
        this.is32LoyaltyTransactionDao = is32LoyaltyTransactionDao;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
