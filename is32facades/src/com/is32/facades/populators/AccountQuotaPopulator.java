package com.is32.facades.populators;

import com.is32.facades.dto.AccountQuotaData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import java.util.Map;

/**
 * Populates an AccountQuotaData DTO from a raw Map result produced by the
 * IS32AccountQuotaService aggregation.
 */
public class AccountQuotaPopulator implements Populator<Map<String, Object>, AccountQuotaData>
{
    @Override
    public void populate(final Map<String, Object> source, final AccountQuotaData target) throws ConversionException
    {
        target.setAccountId((String) source.get("accountId"));
        target.setSiebelAcctId((String) source.get("siebelAcctId"));

        final Integer threshold = (Integer) source.get("threshold");
        target.setThreshold(threshold != null ? threshold : 0);

        final Integer orderedAmt = (Integer) source.get("orderedAmt");
        target.setOrderedAmt(orderedAmt != null ? orderedAmt : 0);

        final int remaining = (threshold != null ? threshold : 0) - (orderedAmt != null ? orderedAmt : 0);
        target.setRemainingQuota(Math.max(remaining, 0));
    }
}
