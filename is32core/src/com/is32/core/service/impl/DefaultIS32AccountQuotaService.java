package com.is32.core.service.impl;

import com.is32.core.dao.IS32AccountQuotaDao;
import com.is32.core.service.IS32AccountQuotaService;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for computing account quotas by aggregating raw row-level data
 * from the multi-join FlexibleSearch query. Converts the flat result set into grouped
 * account quota entries with summed ordered amounts.
 */
public class DefaultIS32AccountQuotaService implements IS32AccountQuotaService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32AccountQuotaService.class);

    private static final String KEY_ACCOUNT_ID = "accountId";
    private static final String KEY_SIEBEL_ACCT_ID = "siebelAcctId";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_ORDERED_AMT = "orderedAmt";

    private IS32AccountQuotaDao is32AccountQuotaDao;
    private UserService userService;
    private CatalogVersionService catalogVersionService;
    private SessionService sessionService;

    @Override
    public List<Map<String, Object>> getAccountQuotas(final CustomerModel customer,
                                                       final CatalogVersionModel catalogVersion)
    {
        final Date currentDate = new Date();
        final List<List<Object>> rawData = is32AccountQuotaDao.findAccountQuotaRawData(
                customer, catalogVersion, currentDate);

        return aggregateQuotaData(rawData);
    }

    @Override
    public List<Map<String, Object>> getAccountQuotasForCurrentUser()
    {
        final UserModel currentUser = userService.getCurrentUser();

        if (!(currentUser instanceof CustomerModel))
        {
            LOG.warn("Current user [" + currentUser.getUid() + "] is not a customer");
            return new ArrayList<>();
        }

        final CatalogVersionModel catalogVersion = catalogVersionService.getSessionCatalogVersions().stream()
                .filter(cv -> "Online".equals(cv.getVersion()))
                .findFirst()
                .orElse(null);

        if (catalogVersion == null)
        {
            LOG.error("No Online catalog version found in session");
            return new ArrayList<>();
        }

        return getAccountQuotas((CustomerModel) currentUser, catalogVersion);
    }

    @Override
    public int getRedemptionCount(final String promotionUid, final CustomerModel customer)
    {
        return is32AccountQuotaDao.getRedemptionCountForCustomer(promotionUid, customer);
    }

    /**
     * Aggregates raw row data into grouped account quota entries. Each raw row contains
     * [accountId, siebelAcctId, threshold, userPk]. Rows are grouped by (accountId,
     * siebelAcctId, threshold) and the ordered amount is computed as the count of rows
     * where userPk is non-null (indicating the coupon was redeemed by the target customer).
     */
    protected List<Map<String, Object>> aggregateQuotaData(final List<List<Object>> rawData)
    {
        final Map<String, Map<String, Object>> aggregatedMap = new LinkedHashMap<>();

        for (final List<Object> row : rawData)
        {
            final String accountId = (String) row.get(0);
            final String siebelAcctId = (String) row.get(1);
            final Integer threshold = (Integer) row.get(2);
            final Object userPk = row.get(3);

            final String groupKey = accountId + "|" + siebelAcctId + "|" + threshold;
            final int isUsed = (userPk != null) ? 1 : 0;

            final Map<String, Object> existing = aggregatedMap.get(groupKey);
            if (existing != null)
            {
                final int currentAmt = (Integer) existing.get(KEY_ORDERED_AMT);
                existing.put(KEY_ORDERED_AMT, currentAmt + isUsed);
            }
            else
            {
                final Map<String, Object> entry = new HashMap<>();
                entry.put(KEY_ACCOUNT_ID, accountId);
                entry.put(KEY_SIEBEL_ACCT_ID, siebelAcctId);
                entry.put(KEY_THRESHOLD, threshold);
                entry.put(KEY_ORDERED_AMT, isUsed);
                aggregatedMap.put(groupKey, entry);
            }
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Aggregated [" + rawData.size() + "] raw rows into ["
                    + aggregatedMap.size() + "] account quota entries");
        }

        return new ArrayList<>(aggregatedMap.values());
    }

    @Required
    public void setIs32AccountQuotaDao(final IS32AccountQuotaDao is32AccountQuotaDao)
    {
        this.is32AccountQuotaDao = is32AccountQuotaDao;
    }

    @Required
    public void setUserService(final UserService userService)
    {
        this.userService = userService;
    }

    @Required
    public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
    {
        this.catalogVersionService = catalogVersionService;
    }

    @Required
    public void setSessionService(final SessionService sessionService)
    {
        this.sessionService = sessionService;
    }
}
