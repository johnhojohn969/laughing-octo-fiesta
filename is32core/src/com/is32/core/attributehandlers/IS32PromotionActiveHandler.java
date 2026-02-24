package com.is32.core.attributehandlers;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import de.hybris.platform.servicelayer.time.TimeService;

import java.util.Date;

/**
 * Dynamic attribute handler for the IS32Promotion.isActive computed attribute.
 * A promotion is considered active when:
 * - Its status is ACTIVE
 * - It is not suspended
 * - The current date falls within the promotion's start/end date range
 */
public class IS32PromotionActiveHandler implements DynamicAttributeHandler<Boolean, IS32PromotionModel>
{
    private TimeService timeService;

    @Override
    public Boolean get(final IS32PromotionModel promotion)
    {
        if (promotion == null)
        {
            return Boolean.FALSE;
        }

        if (!IS32PromotionStatus.ACTIVE.equals(promotion.getStatus()))
        {
            return Boolean.FALSE;
        }

        if (Boolean.TRUE.equals(promotion.getSuspended()))
        {
            return Boolean.FALSE;
        }

        final Date now = timeService.getCurrentTime();
        final Date startDate = promotion.getStartDate();
        final Date endDate = promotion.getEndDate();

        if (startDate == null || endDate == null)
        {
            return Boolean.FALSE;
        }

        return !now.before(startDate) && now.before(endDate);
    }

    @Override
    public void set(final IS32PromotionModel model, final Boolean value)
    {
        throw new UnsupportedOperationException("isActive is a read-only dynamic attribute");
    }

    public void setTimeService(final TimeService timeService)
    {
        this.timeService = timeService;
    }
}
