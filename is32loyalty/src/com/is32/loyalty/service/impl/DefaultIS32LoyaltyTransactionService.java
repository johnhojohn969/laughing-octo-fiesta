package com.is32.loyalty.service.impl;

import com.is32.loyalty.dao.IS32LoyaltyCardDao;
import com.is32.loyalty.dao.IS32LoyaltyTransactionDao;
import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.enums.IS32LoyaltyTransactionType;
import com.is32.loyalty.service.IS32LoyaltyTransactionService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32LoyaltyTransactionService implements IS32LoyaltyTransactionService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32LoyaltyTransactionService.class);

    private IS32LoyaltyTransactionDao is32LoyaltyTransactionDao;
    private IS32LoyaltyCardDao is32LoyaltyCardDao;
    private ModelService modelService;

    @Override
    public IS32LoyaltyTransactionModel getTransactionById(final String transactionId)
    {
        return is32LoyaltyTransactionDao.findByTransactionId(transactionId);
    }

    @Override
    public List<IS32LoyaltyTransactionModel> getTransactionsForCard(final String cardNumber)
    {
        return is32LoyaltyTransactionDao.findByCardNumber(cardNumber);
    }

    @Override
    public List<IS32LoyaltyTransactionModel> getTransactionsByMerchant(final String merchantCode)
    {
        return is32LoyaltyTransactionDao.findByMerchantCode(merchantCode);
    }

    @Override
    public Map<String, Double> getTransactionSummary(final String cardNumber)
    {
        final List<List<Object>> rawData = is32LoyaltyTransactionDao.getTransactionSummaryByCard(cardNumber);
        final Map<String, Double> summary = new HashMap<>();

        double totalAmount = 0;
        double totalEarned = 0;
        double totalRedeemed = 0;
        int transactionCount = 0;

        for (final List<Object> row : rawData)
        {
            final Double amount = (Double) row.get(2);
            final Integer earned = (Integer) row.get(3);
            final Integer redeemed = (Integer) row.get(4);

            if (amount != null) totalAmount += amount;
            if (earned != null) totalEarned += earned;
            if (redeemed != null) totalRedeemed += redeemed;
            transactionCount++;
        }

        summary.put("totalAmount", totalAmount);
        summary.put("totalPointsEarned", totalEarned);
        summary.put("totalPointsRedeemed", totalRedeemed);
        summary.put("transactionCount", (double) transactionCount);
        summary.put("averageTransactionAmount", transactionCount > 0 ? totalAmount / transactionCount : 0.0);

        return summary;
    }

    @Override
    public List<IS32LoyaltyTransactionModel> searchTransactionsByDescription(final String keyword)
    {
        return is32LoyaltyTransactionDao.findByDescriptionKeyword(keyword);
    }

    @Override
    public List<Map<String, Object>> getMerchantReport(final String merchantCode,
                                                        final Date startDate,
                                                        final Date endDate)
    {
        final List<List<Object>> rawData = is32LoyaltyTransactionDao.getMerchantTransactionReport(merchantCode, startDate, endDate);
        final List<Map<String, Object>> report = new ArrayList<>();

        for (final List<Object> row : rawData)
        {
            final Map<String, Object> entry = new HashMap<>();
            entry.put("transactionType", row.get(0));
            entry.put("amount", row.get(1));
            entry.put("pointsEarned", row.get(2));
            entry.put("customerName", row.get(3));
            entry.put("tierCode", row.get(4));
            entry.put("pointBalance", row.get(5));
            entry.put("tierMultiplier", row.get(6));
            report.add(entry);
        }

        return report;
    }

    @Override
    public List<IS32LoyaltyTransactionModel> getRecentTransactionsByCustomerEmail(final String email)
    {
        return is32LoyaltyTransactionDao.findRecentTransactionsByEmail(email);
    }

    public void setIs32LoyaltyTransactionDao(final IS32LoyaltyTransactionDao is32LoyaltyTransactionDao)
    {
        this.is32LoyaltyTransactionDao = is32LoyaltyTransactionDao;
    }

    public void setIs32LoyaltyCardDao(final IS32LoyaltyCardDao is32LoyaltyCardDao)
    {
        this.is32LoyaltyCardDao = is32LoyaltyCardDao;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
