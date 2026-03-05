package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32VoucherRedemptionDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32VoucherRedemptionDao.class);

    private static final String FIND_REDEEMABLE_VOUCHERS =
            "SELECT {v.pk} FROM {IS32Voucher AS v JOIN IS32VoucherPool AS vp ON {vp.voucherCode} = {v.code}} " +
            "WHERE {v.enabled} = ?enabled " +
            "AND {vp.channel} = ?channel " +
            "AND ({{ SELECT COUNT({va.pk}) FROM {IS32VoucherAllocation AS va} WHERE {va.voucherCode} = {v.code} }}) < {v.maxRedemptions} " +
            "AND EXISTS ({{ SELECT {vc.pk} FROM {IS32VoucherCondition AS vc} WHERE {vc.voucherCode} = {v.code} AND {vc.conditionType} = ?condType }}) " +
            "AND NOT EXISTS ({{ SELECT {vb.pk} FROM {IS32VoucherBlacklist AS vb} WHERE {vb.customerId} = ?customerId AND {vb.voucherCode} = {v.code} }}) " +
            "AND {v.validFrom} <= ?now " +
            "AND {v.validTo} >= ?now " +
            "AND {v.storeGroup} = ?storeGroup";

    private static final String FIND_VOUCHERS_BY_CAMPAIGN =
            "SELECT {v.pk} FROM {IS32Voucher AS v} " +
            "WHERE {v.campaignId} = ?campaignId " +
            "AND {v.enabled} = ?enabled " +
            "ORDER BY {v.creationtime} DESC";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findRedeemableVouchers(final String channel, final String condType,
                                                final String customerId, final Date now,
                                                final Object storeGroup)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("enabled", Boolean.TRUE);
        params.put("channel", channel);
        params.put("condType", condType);
        params.put("customerId", customerId);
        params.put("now", now);
        params.put("storeGroup", storeGroup);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_REDEEMABLE_VOUCHERS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findVouchersByCampaign(final String campaignId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("campaignId", campaignId);
        params.put("enabled", Boolean.TRUE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_VOUCHERS_BY_CAMPAIGN, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
