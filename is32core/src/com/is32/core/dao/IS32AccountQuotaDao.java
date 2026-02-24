package com.is32.core.dao;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;

import java.util.Date;
import java.util.List;

public interface IS32AccountQuotaDao
{
    /**
     * Retrieves account quota data by joining IS32Promotion with its associated tags, coupons,
     * coupon redemptions, rewards, activities, buckets, promo items, products, and e-stamp tiers.
     *
     * The result set contains rows of [accountId, siebelAcctId, threshold, isUsed] which
     * must be aggregated by the caller to compute the total ordered amount per account.
     *
     * @param customer         the customer to check redemption status against
     * @param catalogVersion   the catalog version for product filtering
     * @param currentDate      the current date for date range filtering
     * @return list of Object arrays containing [accountId, siebelAcctId, threshold, isUsed]
     */
    List<List<Object>> findAccountQuotaRawData(CustomerModel customer, CatalogVersionModel catalogVersion, Date currentDate);

    /**
     * Retrieves the count of coupon redemptions for a given promotion and customer.
     *
     * @param promotionUid the promotion UID
     * @param customer     the customer
     * @return count of redemptions
     */
    int getRedemptionCountForCustomer(String promotionUid, CustomerModel customer);
}
