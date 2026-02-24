package com.is32.core.interceptors;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.PrepareInterceptor;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class IS32PromotionPrepareInterceptor implements PrepareInterceptor<IS32PromotionModel>
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionPrepareInterceptor.class);

    private KeyGenerator keyGenerator;

    @Override
    public void onPrepare(final IS32PromotionModel promotion, final InterceptorContext ctx) throws InterceptorException
    {
        if (ctx.isNew(promotion) && StringUtils.isBlank(promotion.getUid()))
        {
            final String generatedUid = keyGenerator.generate().toString();
            promotion.setUid(generatedUid);

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Generated UID [" + generatedUid + "] for new IS32Promotion");
            }
        }

        if (ctx.isNew(promotion) && promotion.getStatus() == null)
        {
            promotion.setStatus(IS32PromotionStatus.DRAFT);
        }

        if (promotion.getSuspended() == null)
        {
            promotion.setSuspended(Boolean.FALSE);
        }

        if (promotion.getPriority() == null)
        {
            promotion.setPriority(Integer.valueOf(0));
        }
    }

    public void setKeyGenerator(final KeyGenerator keyGenerator)
    {
        this.keyGenerator = keyGenerator;
    }
}
