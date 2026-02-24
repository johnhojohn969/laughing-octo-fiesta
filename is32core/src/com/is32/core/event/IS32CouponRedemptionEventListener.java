package com.is32.core.event;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32RewardType;
import com.is32.core.service.EStampTierService;
import com.is32.core.service.IS32PromotionService;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.couponservices.model.AbstractCouponModel;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Listens for coupon redemption events and updates e-stamp tier counts when the
 * redeemed coupon is associated with an IS32 Promotion that has a reward of type
 * INCREASE_MEMBER_ACCOUNT.
 */
public class IS32CouponRedemptionEventListener extends AbstractEventListener<IS32CouponRedemptionEvent>
{
    private static final Logger LOG = Logger.getLogger(IS32CouponRedemptionEventListener.class);

    private ModelService modelService;
    private IS32PromotionService is32PromotionService;
    private EStampTierService eStampTierService;

    @Override
    protected void onEvent(final IS32CouponRedemptionEvent event)
    {
        if (event == null || event.getPromotionUid() == null)
        {
            LOG.warn("Received null or invalid coupon redemption event");
            return;
        }

        final String promotionUid = event.getPromotionUid();
        final String customerUid = event.getCustomerUid();

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Processing coupon redemption event for promotion [" + promotionUid
                    + "] and customer [" + customerUid + "]");
        }

        try
        {
            final IS32PromotionModel promotion = is32PromotionService.getPromotionByUid(promotionUid);

            if (promotion == null)
            {
                LOG.warn("No promotion found for uid [" + promotionUid + "], skipping event processing");
                return;
            }

            final Collection<IS32RewardModel> rewards = promotion.getRewards();
            if (rewards == null || rewards.isEmpty())
            {
                return;
            }

            for (final IS32RewardModel reward : rewards)
            {
                if (IS32RewardType.INCREASE_MEMBER_ACCOUNT.equals(reward.getRewardType())
                        && reward.getIncreaseMemberAccountId() != null)
                {
                    final int incrementValue = reward.getRewardValue() != null
                            ? reward.getRewardValue().intValue() : 1;

                    eStampTierService.incrementStampCount(
                            reward.getIncreaseMemberAccountId(), incrementValue);

                    LOG.info("Incremented e-stamp count by [" + incrementValue
                            + "] for account [" + reward.getIncreaseMemberAccountId()
                            + "] via promotion [" + promotionUid + "]");
                }
            }
        }
        catch (final Exception e)
        {
            LOG.error("Error processing coupon redemption event for promotion ["
                    + promotionUid + "]", e);
        }
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }

    public void setIs32PromotionService(final IS32PromotionService is32PromotionService)
    {
        this.is32PromotionService = is32PromotionService;
    }

    public void setEStampTierService(final EStampTierService eStampTierService)
    {
        this.eStampTierService = eStampTierService;
    }
}
