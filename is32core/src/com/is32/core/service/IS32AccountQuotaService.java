package com.is32.core.service;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;

import java.util.List;
import java.util.Map;

public interface IS32AccountQuotaService
{
    /**
     * Retrieves aggregated account quota data for the given customer. Each entry in the returned
     * list represents one account with its quota information.
     *
     * The returned map entries contain:
     *   - "accountId" (String)
     *   - "siebelAcctId" (String)
     *   - "threshold" (Integer)
     *   - "orderedAmt" (Integer) - the total count of coupon redemptions for this account
     *
     * @param customer       the customer whose redemption status to evaluate
     * @param catalogVersion the catalog version for product filtering
     * @return list of account quota entries
     */
    List<Map<String, Object>> getAccountQuotas(CustomerModel customer, CatalogVersionModel catalogVersion);

    /**
     * Retrieves account quotas for the current session user using the active catalog version.
     *
     * @return list of account quota entries
     */
    List<Map<String, Object>> getAccountQuotasForCurrentUser();

    /**
     * Gets the redemption count for a specific promotion and customer.
     *
     * @param promotionUid the promotion UID
     * @param customer     the customer
     * @return redemption count
     */
    int getRedemptionCount(String promotionUid, CustomerModel customer);
}
