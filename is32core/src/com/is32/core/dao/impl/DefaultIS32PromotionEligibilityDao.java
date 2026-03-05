package com.is32.core.dao.impl;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import com.is32.core.enums.IS32RewardType;
import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32PromotionEligibilityDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionEligibilityDao.class);

    // --- PATTERN-01: Complex multi-JOIN with correlated subqueries (COUNT, EXISTS, NOT EXISTS) ---
    // Joins IS32Promotion + IS32Bucket, uses IN-subquery (EXISTS equivalent),
    // NOT IN-subquery (NOT EXISTS equivalent), IS NOT NULL, date range, and 7+ WHERE conditions.
    private static final String FIND_ELIGIBLE_PROMOTIONS =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {IS32Promotion AS p " +
            "JOIN IS32Bucket AS b ON {b.promotionUid} = {p.uid}} " +
            "WHERE {p.redeemDigitalCoupon} IS NOT NULL " +
            "AND {p.status} = ?status " +
            "AND {p.suspended} = ?suspended " +
            "AND {b.bucketType} = ?bucketType " +
            "AND {p.startDate} <= ?currentDate " +
            "AND {p.endDate} >= ?currentDate " +
            "AND {p.uid} IN ({{ " +
            "  SELECT {r.promotionUid} FROM {IS32Reward AS r} " +
            "  WHERE {r.promotionUid} = {p.uid} AND {r.rewardType} = ?rewardType " +
            "}}) " +
            "AND {b.uniqueId} NOT IN ({{ " +
            "  SELECT {pi.bucketUid} FROM {IS32PromoItem AS pi} " +
            "  WHERE {pi.itemCode} = ?excludeItemCode AND {pi.bucketUid} = {b.uniqueId} AND {pi.excludeFlag} = ?trueValue " +
            "}})";

    // --- PATTERN-02: Aggregate function with OR / IS NULL filter ---
    // Uses AVG aggregate, OR condition with IS NULL on the same column,
    // and inequality operator (<>).
    private static final String GET_AVERAGE_REWARD_VALUE =
            "SELECT AVG({r.rewardValue}) " +
            "FROM {IS32Reward AS r} " +
            "WHERE {r.promotionUid} = ?promotionUid " +
            "AND ({r.maxRewardCap} = ?defaultCap OR {r.maxRewardCap} IS NULL) " +
            "AND {r.rewardType} <> ?excludedType";

    // --- PATTERN-03: Bulk PK lookup with IN clause (unbounded parameter list) ---
    // Uses SELECT * and WHERE PK IN with a dynamic collection passed directly.
    private static final String FIND_PROMOTIONS_BY_PK_LIST =
            "SELECT * FROM {IS32Promotion AS p} " +
            "WHERE {p.pk} IN (?pkList)";

    private FlexibleSearchService flexibleSearchService;

    public List<IS32PromotionModel> findEligiblePromotions(final String excludeItemCode,
                                                            final IS32RewardType rewardType,
                                                            final Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", IS32PromotionStatus.ACTIVE);
        params.put("suspended", Boolean.FALSE);
        params.put("bucketType", "INCLUDE");
        params.put("currentDate", currentDate);
        params.put("rewardType", rewardType);
        params.put("excludeItemCode", excludeItemCode);
        params.put("trueValue", Boolean.TRUE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ELIGIBLE_PROMOTIONS, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public Double getAverageRewardValue(final String promotionUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", promotionUid);
        params.put("defaultCap", Double.valueOf(0.0));
        params.put("excludedType", IS32RewardType.POINTS_MULTIPLIER);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_AVERAGE_REWARD_VALUE, params);
        query.setResultClassList(Collections.singletonList(Double.class));

        final SearchResult<Double> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    public List<IS32PromotionModel> findPromotionsByPkList(final List<PK> pkList)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("pkList", pkList);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PROMOTIONS_BY_PK_LIST, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
