package com.is32.loyalty.dao.impl;

import com.is32.loyalty.dao.IS32LoyaltyTransactionDao;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyPointBalanceModel;
import com.is32.loyalty.model.IS32LoyaltyMemberTierModel;
import com.is32.loyalty.enums.IS32LoyaltyTransactionType;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32LoyaltyTransactionDao implements IS32LoyaltyTransactionDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32LoyaltyTransactionDao.class);

    private static final String FIND_BY_TRANSACTION_ID =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.TRANSACTIONID + "} = ?transactionId";

    private static final String FIND_BY_CARD_NUMBER =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = ?cardNumber " +
            "ORDER BY {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} DESC";

    private static final String FIND_BY_MERCHANT_CODE =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.MERCHANTCODE + "} = ?merchantCode";

    private static final String FIND_BY_TRANSACTION_TYPE =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.TRANSACTIONTYPE + "} = ?transactionType";

    private static final String FIND_BY_CARD_AND_DATE_RANGE =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = ?cardNumber " +
            "AND {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} >= ?startDate " +
            "AND {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} <= ?endDate " +
            "ORDER BY {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} DESC";

    private static final String GET_TRANSACTION_SUMMARY =
            "SELECT {t." + IS32LoyaltyTransactionModel.TRANSACTIONTYPE + "}, " +
            "{t." + IS32LoyaltyTransactionModel.MERCHANTCODE + "}, " +
            "{t." + IS32LoyaltyTransactionModel.AMOUNT + "}, " +
            "{t." + IS32LoyaltyTransactionModel.POINTSEARNED + "}, " +
            "{t." + IS32LoyaltyTransactionModel.POINTSREDEEMED + "}, " +
            "{t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = ?cardNumber";

    private static final String FIND_BY_DESCRIPTION_KEYWORD =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.DESCRIPTION + "} LIKE ?keyword";

    private static final String FIND_ALL_FOR_CARD =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "}, " +
            "{c." + IS32LoyaltyCardModel.CUSTOMERNAME + "}, " +
            "{c." + IS32LoyaltyCardModel.TIERCODE + "}, " +
            "{c." + IS32LoyaltyCardModel.EMAIL + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t " +
            "JOIN " + IS32LoyaltyCardModel._TYPECODE + " AS c " +
            "ON {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = {c." + IS32LoyaltyCardModel.CARDNUMBER + "}} " +
            "WHERE {c." + IS32LoyaltyCardModel.CARDNUMBER + "} = ?cardNumber";

    private static final String GET_MERCHANT_REPORT =
            "SELECT {t." + IS32LoyaltyTransactionModel.TRANSACTIONTYPE + "}, " +
            "{t." + IS32LoyaltyTransactionModel.AMOUNT + "}, " +
            "{t." + IS32LoyaltyTransactionModel.POINTSEARNED + "}, " +
            "{c." + IS32LoyaltyCardModel.CUSTOMERNAME + "}, " +
            "{c." + IS32LoyaltyCardModel.TIERCODE + "}, " +
            "{pb." + IS32LoyaltyPointBalanceModel.BALANCE + "}, " +
            "{mt." + IS32LoyaltyMemberTierModel.MULTIPLIER + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t " +
            "JOIN " + IS32LoyaltyCardModel._TYPECODE + " AS c " +
            "ON {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = {c." + IS32LoyaltyCardModel.CARDNUMBER + "} " +
            "LEFT JOIN " + IS32LoyaltyPointBalanceModel._TYPECODE + " AS pb " +
            "ON {pb." + IS32LoyaltyPointBalanceModel.CARDNUMBER + "} = {c." + IS32LoyaltyCardModel.CARDNUMBER + "} " +
            "LEFT JOIN " + IS32LoyaltyMemberTierModel._TYPECODE + " AS mt " +
            "ON {mt." + IS32LoyaltyMemberTierModel.TIERCODE + "} = {c." + IS32LoyaltyCardModel.TIERCODE + "}} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.MERCHANTCODE + "} = ?merchantCode " +
            "AND {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} >= ?startDate " +
            "AND {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} <= ?endDate";

    private static final String FIND_BY_STORE_ID =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t} " +
            "WHERE {t." + IS32LoyaltyTransactionModel.STOREID + "} = ?storeId";

    private static final String FIND_RECENT_BY_EMAIL =
            "SELECT {t." + IS32LoyaltyTransactionModel.PK + "} " +
            "FROM {" + IS32LoyaltyTransactionModel._TYPECODE + " AS t " +
            "JOIN " + IS32LoyaltyCardModel._TYPECODE + " AS c " +
            "ON {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "} = {c." + IS32LoyaltyCardModel.CARDNUMBER + "}} " +
            "WHERE LOWER({c." + IS32LoyaltyCardModel.EMAIL + "}) = LOWER(?email) " +
            "ORDER BY {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} DESC";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public IS32LoyaltyTransactionModel findByTransactionId(final String transactionId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TRANSACTION_ID, params);
        query.setResultClassList(Collections.singletonList(IS32LoyaltyTransactionModel.class));

        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByCardNumber(final String cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardNumber", cardNumber);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CARD_NUMBER, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByMerchantCode(final String merchantCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("merchantCode", merchantCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_MERCHANT_CODE, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByTransactionType(final IS32LoyaltyTransactionType transactionType)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("transactionType", transactionType);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TRANSACTION_TYPE, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByCardNumberAndDateRange(final String cardNumber,
                                                                            final Date startDate,
                                                                            final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardNumber", cardNumber);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CARD_AND_DATE_RANGE, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getTransactionSummaryByCard(final String cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardNumber", cardNumber);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_TRANSACTION_SUMMARY, params);
        query.setResultClassList(Arrays.asList(String.class, String.class, Double.class, Integer.class, Integer.class, Date.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByDescriptionKeyword(final String keyword)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("keyword", "%" + keyword + "%");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_DESCRIPTION_KEYWORD, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findAllTransactionsForCard(final String cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardNumber", cardNumber);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ALL_FOR_CARD, params);
        query.setResultClassList(Arrays.asList(IS32LoyaltyTransactionModel.class, String.class, String.class, String.class));

        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getMerchantTransactionReport(final String merchantCode,
                                                            final Date startDate,
                                                            final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("merchantCode", merchantCode);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_MERCHANT_REPORT, params);
        query.setResultClassList(Arrays.asList(String.class, Double.class, Integer.class, String.class, String.class, Integer.class, Double.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findByStoreId(final String storeId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("storeId", storeId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_STORE_ID, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyTransactionModel> findRecentTransactionsByEmail(final String email)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_RECENT_BY_EMAIL, params);
        final SearchResult<IS32LoyaltyTransactionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
