package com.is32.core.dao.impl;

import com.is32.core.dao.IS32EStampSearchDao;
import com.is32.core.model.EStampTierModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for searching e-stamp tier data with various filter criteria.
 * This DAO demonstrates both correct and incorrect data type usage
 * to test whether the reviewer can distinguish between them.
 */
public class DefaultIS32EStampSearchDao implements IS32EStampSearchDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32EStampSearchDao.class);

    // SLOW-04: accountId is java.lang.String but parameter is int
    private static final String FIND_BY_ACCOUNT_ID_NUMERIC =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.ACCOUNTID + "} = ?accountId";

    // SLOW-04: threshold is java.lang.Integer but parameter is String
    private static final String FIND_BY_THRESHOLD_STR =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.THRESHOLD + "} >= ?threshold " +
            "ORDER BY {et." + EStampTierModel.THRESHOLD + "} ASC";

    // SLOW-04: tierLevel is java.lang.Integer but parameter is String
    private static final String FIND_BY_TIER_LEVEL_STR =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.TIERLEVEL + "} = ?tierLevel " +
            "ORDER BY {et." + EStampTierModel.ACCOUNTID + "}";

    // SLOW-04: siebelAcctId is java.lang.String but parameter is long
    private static final String FIND_BY_SIEBEL_ACCT_ID_NUMERIC =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.SIEBELACCTID + "} = ?siebelAcctId";

    // CORRECT: currentStampCount is java.lang.Integer and parameter is int — NO violation
    private static final String FIND_BY_CURRENT_STAMP_COUNT =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.CURRENTSTAMPCOUNT + "} >= ?currentStampCount " +
            "ORDER BY {et." + EStampTierModel.CURRENTSTAMPCOUNT + "} DESC";

    // CORRECT: tierName is java.lang.String and parameter is String — NO violation
    private static final String FIND_BY_TIER_NAME =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.TIERNAME + "} = ?tierName";

    // SLOW-04: maxStampCount is java.lang.Integer but parameter is String
    private static final String FIND_BY_MAX_STAMP_COUNT_STR =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.MAXSTAMPCOUNT + "} <= ?maxStampCount " +
            "AND {et." + EStampTierModel.CURRENTSTAMPCOUNT + "} < {et." + EStampTierModel.MAXSTAMPCOUNT + "}";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<EStampTierModel> findByAccountIdNumeric(final int accountId)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: accountId column is String, but passing Integer value
        params.put("accountId", Integer.valueOf(accountId));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ACCOUNT_ID_NUMERIC, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByThresholdStr(final String threshold)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: threshold column is Integer, but passing String value
        params.put("threshold", threshold);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_THRESHOLD_STR, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByTierLevelStr(final String tierLevel)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: tierLevel column is Integer, but passing String value
        params.put("tierLevel", tierLevel);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TIER_LEVEL_STR, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findBySiebelAcctIdNumeric(final long siebelAcctId)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: siebelAcctId column is String, but passing Long value
        params.put("siebelAcctId", Long.valueOf(siebelAcctId));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_SIEBEL_ACCT_ID_NUMERIC, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByCurrentStampCount(final int currentStampCount)
    {
        final Map<String, Object> params = new HashMap<>();
        // CORRECT: currentStampCount column is Integer, passing Integer — matches
        params.put("currentStampCount", Integer.valueOf(currentStampCount));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CURRENT_STAMP_COUNT, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByTierName(final String tierName)
    {
        final Map<String, Object> params = new HashMap<>();
        // CORRECT: tierName column is String, passing String — matches
        params.put("tierName", tierName);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TIER_NAME, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByMaxStampCountStr(final String maxStampCount)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: maxStampCount column is Integer, but passing String value
        params.put("maxStampCount", maxStampCount);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_MAX_STAMP_COUNT_STR, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
