package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32PromotionSummaryDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionSummaryDao.class);

    // Simplified query: same core JOIN as SQL-01 (IS32Promotion JOIN IS32Bucket on promotionUid)
    // but only uses COUNT subquery, no EXISTS/NOT EXISTS
    private static final String FIND_PROMOTION_BUCKET_COUNTS =
            "SELECT {p.pk}, {p.name} FROM {IS32Promotion AS p JOIN IS32Bucket AS b ON {b.promotionUid} = {p.uid}} " +
            "WHERE {p.status} = ?status " +
            "AND {p.startDate} <= ?currentDate " +
            "AND {p.endDate} >= ?currentDate " +
            "AND ({{ SELECT COUNT({b2.pk}) FROM {IS32Bucket AS b2} WHERE {b2.promotionUid} = {p.uid} }}) >= ?minBuckets";

    // Even simpler: just the core JOIN, no subqueries at all
    private static final String FIND_ACTIVE_PROMOTION_BUCKETS =
            "SELECT {p.pk}, {b.pk} FROM {IS32Promotion AS p JOIN IS32Bucket AS b ON {b.promotionUid} = {p.uid}} " +
            "WHERE {p.status} = ?status " +
            "AND {b.participateInReward} = ?participateInReward " +
            "AND {p.suspended} = ?suspended " +
            "AND {p.startDate} <= ?currentDate " +
            "AND {p.endDate} >= ?currentDate";

    // Different query with only EXISTS subquery (no COUNT, no NOT EXISTS)
    private static final String FIND_PROMOTIONS_WITH_THRESHOLDS =
            "SELECT {p.pk} FROM {IS32Promotion AS p JOIN IS32Bucket AS b ON {b.promotionUid} = {p.uid}} " +
            "WHERE {p.redeemDigitalCoupon} IS NOT NULL " +
            "AND EXISTS ({{ SELECT {t.pk} FROM {IS32Threshold AS t} WHERE {t.promotionUid} = {p.uid} AND {t.thresholdType} = ?thresholdType }}) " +
            "AND {p.status} = ?status " +
            "AND {p.baseStore} = ?baseStore";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findPromotionBucketCounts(final String status, final Date currentDate, final int minBuckets)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("currentDate", currentDate);
        params.put("minBuckets", minBuckets);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PROMOTION_BUCKET_COUNTS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findActivePromotionBuckets(final String status, final boolean participateInReward,
                                                    final boolean suspended, final Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("participateInReward", participateInReward);
        params.put("suspended", suspended);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACTIVE_PROMOTION_BUCKETS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findPromotionsWithThresholds(final String thresholdType, final String status,
                                                      final Object baseStore)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("thresholdType", thresholdType);
        params.put("status", status);
        params.put("baseStore", baseStore);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PROMOTIONS_WITH_THRESHOLDS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
