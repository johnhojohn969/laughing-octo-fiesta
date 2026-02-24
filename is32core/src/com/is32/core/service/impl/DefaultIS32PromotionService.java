package com.is32.core.service.impl;

import com.is32.core.dao.IS32AccountQuotaDao;
import com.is32.core.dao.IS32PromotionDao;
import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import com.is32.core.service.IS32PromotionService;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.time.TimeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.Date;
import java.util.List;

public class DefaultIS32PromotionService implements IS32PromotionService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionService.class);

    private IS32PromotionDao is32PromotionDao;
    private IS32AccountQuotaDao is32AccountQuotaDao;
    private ModelService modelService;
    private SessionService sessionService;
    private CatalogVersionService catalogVersionService;

    @Override
    public IS32PromotionModel getPromotionByUid(final String uid)
    {
        final IS32PromotionModel promotion = is32PromotionDao.findByUid(uid);
        if (promotion == null)
        {
            LOG.warn("No IS32Promotion found with uid [" + uid + "]");
        }
        return promotion;
    }

    @Override
    public List<IS32PromotionModel> getPromotionsByStatus(final IS32PromotionStatus status)
    {
        return is32PromotionDao.findByStatus(status);
    }

    @Override
    public List<IS32PromotionModel> getActivePromotions()
    {
        return is32PromotionDao.findActivePromotions(new Date());
    }

    @Override
    public List<IS32PromotionModel> getActiveNonSuspendedPromotions()
    {
        return is32PromotionDao.findActiveNonSuspendedPromotions(new Date());
    }

    @Override
    public List<IS32PromotionModel> getPromotionsByTagCode(final String tagCode)
    {
        return is32PromotionDao.findByPromotionTagCode(tagCode);
    }

    @Override
    public List<IS32PromotionModel> getExpiredPromotions(final int maxResults)
    {
        return is32PromotionDao.findExpiredPromotions(new Date(), maxResults);
    }

    @Override
    public List<IS32PromotionModel> getPromotionsByDateRange(final Date startDate, final Date endDate)
    {
        return is32PromotionDao.findPromotionsByDateRange(startDate, endDate);
    }

    @Override
    public List<IS32PromotionModel> getPromotionsForCatalogVersion(final CatalogVersionModel catalogVersion)
    {
        return is32PromotionDao.findPromotionsForCatalogVersion(catalogVersion, new Date());
    }

    @Override
    public void updatePromotionStatus(final IS32PromotionModel promotion, final IS32PromotionStatus newStatus)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Updating promotion [" + promotion.getUid() + "] status from ["
                    + promotion.getStatus() + "] to [" + newStatus + "]");
        }
        promotion.setStatus(newStatus);
        modelService.save(promotion);
    }

    @Override
    public void suspendPromotion(final IS32PromotionModel promotion)
    {
        promotion.setSuspended(Boolean.TRUE);
        modelService.save(promotion);
        LOG.info("Promotion [" + promotion.getUid() + "] has been suspended");
    }

    @Override
    public void resumePromotion(final IS32PromotionModel promotion)
    {
        promotion.setSuspended(Boolean.FALSE);
        modelService.save(promotion);
        LOG.info("Promotion [" + promotion.getUid() + "] has been resumed");
    }

    @Override
    public void markExpiredPromotions()
    {
        final List<IS32PromotionModel> expiredPromotions = is32PromotionDao.findExpiredPromotions(new Date(), Integer.MAX_VALUE);

        int count = 0;
        for (final IS32PromotionModel promotion : expiredPromotions)
        {
            if (!IS32PromotionStatus.EXPIRED.equals(promotion.getStatus()))
            {
                promotion.setStatus(IS32PromotionStatus.EXPIRED);
                modelService.save(promotion);
                count++;
            }
        }

        LOG.info("Marked [" + count + "] promotions as expired");
    }

    @Required
    public void setIs32PromotionDao(final IS32PromotionDao is32PromotionDao)
    {
        this.is32PromotionDao = is32PromotionDao;
    }

    @Required
    public void setIs32AccountQuotaDao(final IS32AccountQuotaDao is32AccountQuotaDao)
    {
        this.is32AccountQuotaDao = is32AccountQuotaDao;
    }

    @Required
    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }

    @Required
    public void setSessionService(final SessionService sessionService)
    {
        this.sessionService = sessionService;
    }

    @Required
    public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
    {
        this.catalogVersionService = catalogVersionService;
    }
}
