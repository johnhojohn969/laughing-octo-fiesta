package com.is32.loyalty.dao.impl;

import com.is32.loyalty.dao.IS32LoyaltyCardDao;
import com.is32.loyalty.model.IS32LoyaltyCardModel;
import com.is32.loyalty.model.IS32LoyaltyTransactionModel;
import com.is32.loyalty.enums.IS32LoyaltyCardStatus;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32LoyaltyCardDao implements IS32LoyaltyCardDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32LoyaltyCardDao.class);

    private static final String FIND_BY_CARD_NUMBER =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.CARDNUMBER + "} = ?cardNumber";

    private static final String FIND_BY_CUSTOMER_ID =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.CUSTOMERID + "} = ?customerId";

    private static final String FIND_BY_STATUS =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.STATUS + "} = ?status";

    private static final String FIND_BY_NAME_PATTERN =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.CUSTOMERNAME + "} LIKE ?namePattern";

    private static final String FIND_ACTIVE_WITH_TRANSACTIONS =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c " +
            "JOIN " + IS32LoyaltyTransactionModel._TYPECODE + " AS t " +
            "ON {c." + IS32LoyaltyCardModel.CARDNUMBER + "} = {t." + IS32LoyaltyTransactionModel.CARDNUMBER + "}} " +
            "WHERE {c." + IS32LoyaltyCardModel.STATUS + "} = ?status " +
            "AND {t." + IS32LoyaltyTransactionModel.TRANSACTIONDATE + "} >= ?fromDate";

    private static final String FIND_BY_EMAIL =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE LOWER({c." + IS32LoyaltyCardModel.EMAIL + "}) = LOWER(?email)";

    private static final String FIND_EXPIRED_CARDS =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.EXPIRYDATE + "} < ?referenceDate " +
            "AND {c." + IS32LoyaltyCardModel.STATUS + "} <> ?expiredStatus " +
            "ORDER BY {c." + IS32LoyaltyCardModel.EXPIRYDATE + "} ASC";

    private static final String FIND_BY_TIER_AND_STATUS =
            "SELECT {c." + IS32LoyaltyCardModel.PK + "} " +
            "FROM {" + IS32LoyaltyCardModel._TYPECODE + " AS c} " +
            "WHERE {c." + IS32LoyaltyCardModel.TIERCODE + "} = ?tierCode " +
            "AND {c." + IS32LoyaltyCardModel.STATUS + "} = ?status";

    private FlexibleSearchService flexibleSearchService;
    private ModelService modelService;

    @Override
    public IS32LoyaltyCardModel findByCardNumber(final String cardNumber)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardNumber", cardNumber);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CARD_NUMBER, params);
        query.setResultClassList(Collections.singletonList(IS32LoyaltyCardModel.class));

        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<IS32LoyaltyCardModel> findByCustomerId(final String customerId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CUSTOMER_ID, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findByStatus(final IS32LoyaltyCardStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_STATUS, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findCardsByCustomerNamePattern(final String namePattern)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("namePattern", "%" + namePattern + "%");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_NAME_PATTERN, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findActiveCardsWithTransactions(final Date fromDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", IS32LoyaltyCardStatus.ACTIVE);
        params.put("fromDate", fromDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACTIVE_WITH_TRANSACTIONS, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findByEmailAddress(final String email)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_EMAIL, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findExpiredCards(final Date referenceDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("referenceDate", referenceDate);
        params.put("expiredStatus", IS32LoyaltyCardStatus.EXPIRED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_EXPIRED_CARDS, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32LoyaltyCardModel> findCardsByTierAndStatus(final String tierCode, final IS32LoyaltyCardStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("tierCode", tierCode);
        params.put("status", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TIER_AND_STATUS, params);
        final SearchResult<IS32LoyaltyCardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
