package com.is32.loyalty.dao.impl;

import com.is32.loyalty.dao.IS32LoyaltyReportDao;
import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.model.IS32LoyaltyPointBalanceModel;
import com.is32.loyalty.enums.IS32LoyaltyCardStatus;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report DAO for loyalty card and transaction analytics.
 * Provides queries for generating various loyalty reports.
 */
public class DefaultIS32LoyaltyReportDao implements IS32LoyaltyReportDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32LoyaltyReportDao.class);

    // SLOW-04: cardNumber is java.lang.String in items.xml but parameter is long
    private static final String FIND_CARDS_BY_NUMERIC_CARD_NUMBER =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.CARDNUMBER + "} = ?cardNumber";

    // SLOW-04: totalPointsEarned is java.lang.Long but parameter is String
    private static final String FIND_CARDS_BY_TOTAL_POINTS =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.TOTALPOINTSEARNED + "} >= ?minPoints " +
            "AND {c." + IS32LoyaltyCardModel.STATUS + "} = ?status " +
            "ORDER BY {c." + IS32LoyaltyCardModel.TOTALPOINTSEARNED + "} DESC";

    // SLOW-04: merchantCode is java.lang.String but parameter is int
    private static final String FIND_TRANSACTIONS_BY_MERCHANT_ID =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.MERCHANTCODE + "} = ?merchantCode";

    // SLOW-04: amount is java.lang.Double but parameters are String
    private static final String FIND_TRANSACTIONS_BY_AMOUNT_RANGE =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.AMOUNT + "} >= ?minAmount " +
            "AND {t." + IS32LoyaltyTransactionModel.AMOUNT + "} <= ?maxAmount " +
            "ORDER BY {t." + IS32LoyaltyTransactionModel.AMOUNT + "} DESC";

    // SLOW-04: pointsEarned is java.lang.Integer but parameter is String
    private static final String FIND_TRANSACTIONS_BY_POINTS_EARNED =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.POINTSEARNED + "} >= ?points";

    // SLOW-04: customerId is java.lang.String but parameter is long
    private static final String FIND_CARDS_BY_CUSTOMER_ID_NUMERIC =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.CUSTOMERID + "} = ?customerId " +
            "AND {c." + IS32LoyaltyCardModel.STATUS + "} = ?status";

    // SLOW-04: cardNumber in both tables is String but parameter is int
    // Also joins on String columns with int comparison
    private static final String GET_CARD_BALANCE_REPORT =
            "SELECT {c." + IS32LoyaltyCardModel.CARDNUMBER + "}, " +
            "{c." + IS32LoyaltyCardModel.CUSTOMERNAME + "}, " +
            "{c." + IS32LoyaltyCardModel.TOTALPOINTSEARNED + "}, " +
            "{pb." + IS32LoyaltyPointBalanceModel.BALANCE + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c " +
            "JOIN " + IS32LoyaltyPointBalanceModel._TYPECODE + " AS pb " +
            "ON {pb." + IS32LoyaltyPointBalanceModel.CARDNUMBER + "} = {c." + IS32LoyaltyCardModel.CARDNUMBER + "}} " +
            "WHERE {c." + IS32LoyaltyCardModel.CARDNUMBER + "} = ?cardNumber";

    // SLOW-04: orderCode is java.lang.String in IS32LoyaltyTransaction but parameter is int
    private static final String FIND_TRANSACTIONS_BY_ORDER_CODE =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.ORDERCODE + "} = ?orderCode";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<IS32LoyaltyCardModel> findCardsByNumericCardNumber(final long cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: cardNumber column is String, but passing Long value
        params.put("cardNumber", Long.valueOf(cardNumber));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARDS_BY_NUMERIC_CARD_NUMBER, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findCardsByTotalPointsEarned(final String minPoints)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: totalPointsEarned column is Long, but passing String value
        params.put("minPoints", minPoints);
        params.put("status", IS32LoyaltyCardStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARDS_BY_TOTAL_POINTS, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findTransactionsByMerchantId(final int merchantCode)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: merchantCode column is String, but passing Integer value
        params.put("merchantCode", Integer.valueOf(merchantCode));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_TRANSACTIONS_BY_MERCHANT_ID, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findTransactionsByAmountRange(final String minAmount,
                                                                             final String maxAmount)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: amount column is Double, but passing String values
        params.put("minAmount", minAmount);
        params.put("maxAmount", maxAmount);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_TRANSACTIONS_BY_AMOUNT_RANGE, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findTransactionsByPointsEarned(final String points)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: pointsEarned column is Integer, but passing String value
        params.put("points", points);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_TRANSACTIONS_BY_POINTS_EARNED, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findCardsByCustomerIdNumeric(final long customerId)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: customerId column is String, but passing Long value
        params.put("customerId", Long.valueOf(customerId));
        params.put("status", IS32LoyaltyCardStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARDS_BY_CUSTOMER_ID_NUMERIC, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getCardBalanceReport(final int cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: cardNumber column is String, but passing Integer value
        params.put("cardNumber", Integer.valueOf(cardNumber));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_CARD_BALANCE_REPORT, params);
        query.setResultClassList(Arrays.asList(String.class, String.class, Long.class, Integer.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findTransactionsByOrderCode(final int orderCode)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: orderCode column is String, but passing Integer value
        params.put("orderCode", Integer.valueOf(orderCode));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_TRANSACTIONS_BY_ORDER_CODE, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
