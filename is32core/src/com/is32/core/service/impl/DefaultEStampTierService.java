package com.is32.core.service.impl;

import com.is32.core.dao.EStampTierDao;
import com.is32.core.model.EStampTierModel;
import com.is32.core.service.EStampTierService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

public class DefaultEStampTierService implements EStampTierService
{
    private static final Logger LOG = Logger.getLogger(DefaultEStampTierService.class);

    private EStampTierDao eStampTierDao;
    private ModelService modelService;

    @Override
    public EStampTierModel getTierByAccountId(final String accountId)
    {
        final EStampTierModel tier = eStampTierDao.findByAccountId(accountId);
        if (tier == null)
        {
            LOG.warn("No EStampTier found for account [" + accountId + "]");
        }
        return tier;
    }

    @Override
    public List<EStampTierModel> getTiersBySiebelAcctId(final String siebelAcctId)
    {
        return eStampTierDao.findBySiebelAcctId(siebelAcctId);
    }

    @Override
    public List<EStampTierModel> getTiersByLevel(final int tierLevel)
    {
        return eStampTierDao.findByTierLevel(tierLevel);
    }

    @Override
    public List<EStampTierModel> getActiveTiers()
    {
        return eStampTierDao.findAllActiveTiers();
    }

    @Override
    public void incrementStampCount(final String accountId, final int count)
    {
        final EStampTierModel tier = eStampTierDao.findByAccountId(accountId);
        if (tier == null)
        {
            LOG.error("Cannot increment stamp count: no tier found for account [" + accountId + "]");
            return;
        }

        final int newCount = (tier.getCurrentStampCount() != null ? tier.getCurrentStampCount() : 0) + count;
        final int maxCount = tier.getMaxStampCount() != null ? tier.getMaxStampCount() : Integer.MAX_VALUE;

        tier.setCurrentStampCount(Math.min(newCount, maxCount));
        modelService.save(tier);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Incremented stamp count for account [" + accountId
                    + "] by [" + count + "] to [" + tier.getCurrentStampCount() + "]");
        }
    }

    @Override
    public void resetStampCount(final String accountId)
    {
        final EStampTierModel tier = eStampTierDao.findByAccountId(accountId);
        if (tier != null)
        {
            tier.setCurrentStampCount(Integer.valueOf(0));
            modelService.save(tier);
            LOG.info("Reset stamp count for account [" + accountId + "]");
        }
    }

    @Override
    public void saveTier(final EStampTierModel tier)
    {
        modelService.save(tier);
    }

    @Required
    public void setEStampTierDao(final EStampTierDao eStampTierDao)
    {
        this.eStampTierDao = eStampTierDao;
    }

    @Required
    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
