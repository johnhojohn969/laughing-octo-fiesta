package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32PromotionBucketDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionBucketDao.class);

    // FSQL-01, FSQL-04, FSQL-05 violation: nested subqueries with EXISTS/NOT EXISTS/COUNT + multi-table JOIN
    private static final String FIND_ELIGIBLE_PROMOTIONS =
            "SELECT {p.pk} FROM {IS32Promotion AS p JOIN IS32Bucket AS b ON {b.promotionUid} = {p.uid}} " +
            "WHERE {p.redeemDigitalCoupon} IS NOT NULL " +
            "AND {p.requiredCoupon} = ?requiredCoupon " +
            "AND {b.participateInReward} = ?participateInReward " +
            "AND ({{ SELECT COUNT({b2.pk}) FROM {IS32Bucket AS b2} WHERE {b2.promotionUid} = {p.uid} }}) = ?bucketCount " +
            "AND EXISTS ({{ SELECT {t.pk} FROM {IS32Threshold AS t} WHERE {t.promotionUid} = {p.uid} AND {t.thresholdType} = ?thresholdType }}) " +
            "AND NOT EXISTS ({{ SELECT {ex.pk} FROM {IS32PromoExcludeItem AS ex} WHERE {ex.itemCode} = ?itemCode AND {ex.bucketUid} = {b.uniqueId} }}) " +
            "AND {p.status} = ?status " +
            "AND {p.suspended} = ?suspended " +
            "AND {p.startDate} <= ?currentDate " +
            "AND {p.endDate} >= ?currentDate " +
            "AND {p.baseStore} = ?baseStore";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findEligiblePromotions(final String requiredCoupon, final boolean participateInReward,
                                                final int bucketCount, final String thresholdType,
                                                final String itemCode, final String status,
                                                final boolean suspended, final Date currentDate,
                                                final Object baseStore)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("requiredCoupon", requiredCoupon);
        params.put("participateInReward", participateInReward);
        params.put("bucketCount", bucketCount);
        params.put("thresholdType", thresholdType);
        params.put("itemCode", itemCode);
        params.put("status", status);
        params.put("suspended", suspended);
        params.put("currentDate", currentDate);
        params.put("baseStore", baseStore);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ELIGIBLE_PROMOTIONS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
