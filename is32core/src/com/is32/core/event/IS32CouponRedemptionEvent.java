package com.is32.core.event;

import de.hybris.platform.servicelayer.event.events.AbstractEvent;

/**
 * Event published when a coupon associated with an IS32 Promotion is redeemed.
 * Carries the promotion UID and customer UID for downstream processing.
 */
public class IS32CouponRedemptionEvent extends AbstractEvent
{
    private final String promotionUid;
    private final String customerUid;
    private final String couponCode;

    public IS32CouponRedemptionEvent(final String promotionUid, final String customerUid, final String couponCode)
    {
        super();
        this.promotionUid = promotionUid;
        this.customerUid = customerUid;
        this.couponCode = couponCode;
    }

    public String getPromotionUid()
    {
        return promotionUid;
    }

    public String getCustomerUid()
    {
        return customerUid;
    }

    public String getCouponCode()
    {
        return couponCode;
    }
}
