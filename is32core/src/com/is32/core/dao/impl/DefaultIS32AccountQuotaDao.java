package com.is32.core.dao.impl;

import com.is32.core.dao.IS32AccountQuotaDao;
import com.is32.core.enums.IS32PromotionDisplayType;
import com.is32.core.enums.IS32PromotionStatus;
import com.is32.core.enums.IS32RewardType;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for retrieving account quota data by performing a multi-table join across the IS32 promotion
 * model graph: IS32Promotion -> IS32PromotionTag, Coupon, CouponRedemption, Customer,
 * IS32Reward, IS32PromotionActivity, IS32Bucket, IS32PromoItem, Product, and EStampTier.
 *
 * This query is intentionally comprehensive to capture the full promotion qualification chain
 * including coupon redemption status per customer, reward configuration, activity time windows,
 * bucket/item mappings, and e-stamp tier thresholds.
 */
public class DefaultIS32AccountQuotaDao implements IS32AccountQuotaDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32AccountQuotaDao.class);

    /**
     * Main account quota query.
     *
     * Joins 12 tables to resolve the full promotion-to-tier chain and determine per-account
     * coupon redemption status. The result rows contain:
     *   [0] accountId (String) - from IS32Reward.increaseMemberAccountId
     *   [1] siebelAcctId (String) - from EStampTier.siebelAcctId
     *   [2] threshold (Integer) - from EStampTier.threshold
     *   [3] userPk (PK or null) - from Customer LEFT JOIN, null means not redeemed by this user
     *
     * The caller must aggregate these rows: GROUP BY accountId, siebelAcctId, threshold
     * and SUM the isUsed flag (non-null userPk = 1, null = 0) to get orderedAmt.
     */
    private static final String ACCOUNT_QUOTA_QUERY =
            "SELECT {r.increaseMemberAccountId}, {et.siebelAcctId}, {et.threshold}, {u.pk} " +
            "FROM { " +
                "IS32Promotion AS p " +
                "JOIN IS32PromotionTag AS pt ON {p.promotionTag} = {pt.pk} " +
                "JOIN Coupon AS c ON {c.couponId} = {p.redeemDigitalCoupon} " +
                "LEFT JOIN CouponRedemption AS cr ON {c.pk} = {cr.coupon} " +
                "LEFT JOIN Customer AS u ON ({cr.user} = {u.pk} AND {u.pk} = ?userPk) " +
                "JOIN IS32Reward AS r ON {p.uid} = {r.promotionUid} " +
                "JOIN IS32PromotionActivity AS pa ON {pa.promotionUid} = {p.uid} " +
                "JOIN IS32Bucket AS b ON {p.uid} = {b.promotionUid} " +
                "JOIN IS32PromoItem AS pi ON {pi.bucketUid} = {b.uniqueId} " +
                "JOIN Product AS prod ON {prod.code} = {pi.itemCode} " +
                "JOIN EStampTier AS et ON {r.increaseMemberAccountId} = {et.accountId} " +
            "} " +
            "WHERE {pt.elabPromotionDisplayType} = ?displayType " +
            "AND {r.rewardType} = ?rewardType " +
            "AND {p.status} = ?status " +
            "AND {p.suspended} = ?suspended " +
            "AND {prod.catalogVersion} = ?catalogVersion " +
            "AND {p.startDate} <= ?currentDate " +
            "AND {p.endDate} > ?currentDate " +
            "AND {pa.startTime} <= ?currentDate " +
            "AND {pa.endTime} > ?currentDate " +
            "AND ({prod.onlineDate} IS NULL OR {prod.onlineDate} <= ?currentDate) " +
            "AND ({prod.offlineDate} IS NULL OR {prod.offlineDate} >= ?currentDate) " +
            "ORDER BY {r.increaseMemberAccountId}";

    private static final String REDEMPTION_COUNT_QUERY =
            "SELECT COUNT({cr.pk}) " +
            "FROM { " +
                "IS32Promotion AS p " +
                "JOIN Coupon AS c ON {c.couponId} = {p.redeemDigitalCoupon} " +
                "JOIN CouponRedemption AS cr ON {c.pk} = {cr.coupon} " +
            "} " +
            "WHERE {p.uid} = ?promotionUid " +
            "AND {cr.user} = ?userPk";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<List<Object>> findAccountQuotaRawData(final CustomerModel customer,
                                                       final CatalogVersionModel catalogVersion,
                                                       final Date currentDate)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Executing account quota query for customer [" + customer.getUid()
                    + "] with catalog version [" + catalogVersion.getVersion() + "]");
        }

        final Map<String, Object> params = new HashMap<>();
        params.put("userPk", customer.getPk());
        params.put("displayType", IS32PromotionDisplayType.ESTAMP);
        params.put("rewardType", IS32RewardType.INCREASE_MEMBER_ACCOUNT);
        params.put("status", IS32PromotionStatus.ACTIVE);
        params.put("suspended", Boolean.FALSE);
        params.put("catalogVersion", catalogVersion);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(ACCOUNT_QUOTA_QUERY, params);
        fsQuery.setResultClassList(Arrays.asList(String.class, String.class, Integer.class, Object.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(fsQuery);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Account quota query returned [" + result.getTotalCount() + "] raw rows");
        }

        return result.getResult();
    }

    @Override
    public int getRedemptionCountForCustomer(final String promotionUid, final CustomerModel customer)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", promotionUid);
        params.put("userPk", customer.getPk());

        final FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(REDEMPTION_COUNT_QUERY, params);
        fsQuery.setResultClassList(Arrays.asList(Integer.class));

        final SearchResult<Integer> result = flexibleSearchService.search(fsQuery);
        return result.getResult().isEmpty() ? 0 : result.getResult().get(0);
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
