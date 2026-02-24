package com.is32.core.interceptors;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import org.apache.log4j.Logger;

import java.util.Date;

public class IS32PromotionValidateInterceptor implements ValidateInterceptor<IS32PromotionModel>
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionValidateInterceptor.class);

    @Override
    public void onValidate(final IS32PromotionModel promotion, final InterceptorContext ctx)
            throws InterceptorException
    {
        validateDateRange(promotion);
        validateRedemptionLimits(promotion);
        validateStatusTransition(promotion, ctx);
    }

    private void validateDateRange(final IS32PromotionModel promotion) throws InterceptorException
    {
        final Date startDate = promotion.getStartDate();
        final Date endDate = promotion.getEndDate();

        if (startDate != null && endDate != null && !endDate.after(startDate))
        {
            throw new InterceptorException(
                    "IS32Promotion end date must be after start date. Start: ["
                    + startDate + "], End: [" + endDate + "]");
        }
    }

    private void validateRedemptionLimits(final IS32PromotionModel promotion) throws InterceptorException
    {
        final Integer maxPerUser = promotion.getMaxRedemptionPerUser();
        final Integer totalLimit = promotion.getTotalRedemptionLimit();

        if (maxPerUser != null && maxPerUser <= 0)
        {
            throw new InterceptorException(
                    "IS32Promotion maxRedemptionPerUser must be positive, got [" + maxPerUser + "]");
        }

        if (totalLimit != null && totalLimit <= 0)
        {
            throw new InterceptorException(
                    "IS32Promotion totalRedemptionLimit must be positive, got [" + totalLimit + "]");
        }

        if (maxPerUser != null && totalLimit != null && maxPerUser > totalLimit)
        {
            throw new InterceptorException(
                    "IS32Promotion maxRedemptionPerUser [" + maxPerUser
                    + "] cannot exceed totalRedemptionLimit [" + totalLimit + "]");
        }
    }

    private void validateStatusTransition(final IS32PromotionModel promotion, final InterceptorContext ctx)
            throws InterceptorException
    {
        if (ctx.isNew(promotion))
        {
            return;
        }

        final IS32PromotionStatus status = promotion.getStatus();

        if (IS32PromotionStatus.ACTIVE.equals(status))
        {
            if (promotion.getStartDate() == null || promotion.getEndDate() == null)
            {
                throw new InterceptorException(
                        "IS32Promotion cannot be set to ACTIVE without both startDate and endDate defined");
            }

            if (promotion.getPromotionTag() == null)
            {
                throw new InterceptorException(
                        "IS32Promotion cannot be set to ACTIVE without a promotionTag assigned");
            }
        }
    }
}
