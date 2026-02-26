package com.is32.core.dao.impl;

import com.is32.core.dao.IS32PromotionSearchDao;
import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32PromotionTagModel;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32PromotionStatus;
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
 * DAO for advanced promotion search operations.
 * Contains various query patterns for searching promotions by different criteria.
 */
public class DefaultIS32PromotionSearchDao implements IS32PromotionSearchDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionSearchDao.class);

    // SLOW-04 violation: priority is java.lang.Integer in items.xml but parameter is String
    private static final String FIND_BY_PRIORITY =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.PRIORITY + "} = ?priority";

    // SLOW-04 violation: uid is java.lang.String in items.xml but parameter is Long
    private static final String FIND_BY_UID_NUMERIC =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.UID + "} = ?promotionId";

    // SLOW-04 violation: maxRedemptionPerUser is java.lang.Integer but parameter is String
    private static final String FIND_BY_MAX_REDEMPTION =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.MAXREDEMPTIONPERUSER + "} = ?maxRedemption " +
            "AND {p." + IS32PromotionModel.STATUS + "} = ?status";

    // SLOW-04 violation: totalRedemptionLimit is java.lang.Integer but parameter is String
    private static final String FIND_BY_TOTAL_REDEMPTION_LIMIT =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.TOTALREDEMPTIONLIMIT + "} <= ?limit " +
            "AND {p." + IS32PromotionModel.STATUS + "} = ?status " +
            "ORDER BY {p." + IS32PromotionModel.PRIORITY + "} DESC";

    // SLOW-04 violation: priority is java.lang.Integer but parameters are String
    private static final String FIND_BY_PRIORITY_RANGE =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.PRIORITY + "} >= ?minPriority " +
            "AND {p." + IS32PromotionModel.PRIORITY + "} <= ?maxPriority " +
            "AND {p." + IS32PromotionModel.STATUS + "} = ?status";

    // SLOW-04 violation: rewardValue is java.lang.Double but parameter minRewardValue is String
    private static final String FIND_WITH_REWARD_VALUE =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p " +
            "JOIN " + IS32RewardModel._TYPECODE + " AS r " +
            "ON {r." + IS32RewardModel.PROMOTIONUID + "} = {p." + IS32PromotionModel.UID + "}} " +
            "WHERE {r." + IS32RewardModel.REWARDVALUE + "} >= ?minRewardValue " +
            "AND {p." + IS32PromotionModel.STATUS + "} = ?status";

    // SLOW-04 violation: promotionTag.code is java.lang.String but parameter tagCode is int
    private static final String GET_PROMOTION_SUMMARY_BY_TAG =
            "SELECT {p." + IS32PromotionModel.UID + "}, " +
            "{p." + IS32PromotionModel.PRIORITY + "}, " +
            "{p." + IS32PromotionModel.STATUS + "}, " +
            "{pt." + IS32PromotionTagModel.CODE + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p " +
            "JOIN " + IS32PromotionTagModel._TYPECODE + " AS pt " +
            "ON {p." + IS32PromotionModel.PROMOTIONTAG + "} = {pt." + IS32PromotionTagModel.PK + "}} " +
            "WHERE {pt." + IS32PromotionTagModel.CODE + "} = ?tagCode";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<IS32PromotionModel> findByPriority(final String priority)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: priority column is Integer, but passing String value
        params.put("priority", priority);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_PRIORITY, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findByUidNumeric(final long promotionId)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: uid column is String, but passing Long value
        params.put("promotionId", Long.valueOf(promotionId));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_UID_NUMERIC, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findByMaxRedemption(final String maxRedemption)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: maxRedemptionPerUser is Integer, but passing String
        params.put("maxRedemption", maxRedemption);
        params.put("status", IS32PromotionStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_MAX_REDEMPTION, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findByTotalRedemptionLimit(final String limit)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: totalRedemptionLimit is Integer, but passing String
        params.put("limit", limit);
        params.put("status", IS32PromotionStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TOTAL_REDEMPTION_LIMIT, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findByPriorityRange(final String minPriority, final String maxPriority)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: priority is Integer, but both parameters are String
        params.put("minPriority", minPriority);
        params.put("maxPriority", maxPriority);
        params.put("status", IS32PromotionStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_PRIORITY_RANGE, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findPromotionsWithRewardValue(final String minRewardValue)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: rewardValue is Double, but passing String
        params.put("minRewardValue", minRewardValue);
        params.put("status", IS32PromotionStatus.ACTIVE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_WITH_REWARD_VALUE, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getPromotionSummaryByTag(final int tagCode)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: promotionTag.code is String, but passing int
        params.put("tagCode", Integer.valueOf(tagCode));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_PROMOTION_SUMMARY_BY_TAG, params);
        query.setResultClassList(Arrays.asList(String.class, Integer.class, String.class, String.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
