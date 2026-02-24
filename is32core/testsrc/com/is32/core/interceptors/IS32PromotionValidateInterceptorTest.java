package com.is32.core.interceptors;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32PromotionTagModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;

import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class IS32PromotionValidateInterceptorTest
{
    @InjectMocks
    private IS32PromotionValidateInterceptor interceptor;

    @Mock
    private InterceptorContext context;

    @Mock
    private IS32PromotionModel promotion;

    @Mock
    private IS32PromotionTagModel tag;

    private Date futureDate;
    private Date pastDate;

    @Before
    public void setUp()
    {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        futureDate = cal.getTime();

        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -30);
        pastDate = cal.getTime();

        when(context.isNew(promotion)).thenReturn(false);
    }

    @Test
    public void testValidPromotion() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(new Date());
        when(promotion.getEndDate()).thenReturn(futureDate);
        when(promotion.getStatus()).thenReturn(IS32PromotionStatus.ACTIVE);
        when(promotion.getPromotionTag()).thenReturn(tag);

        interceptor.onValidate(promotion, context);
    }

    @Test(expected = InterceptorException.class)
    public void testEndDateBeforeStartDate() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(futureDate);
        when(promotion.getEndDate()).thenReturn(pastDate);

        interceptor.onValidate(promotion, context);
    }

    @Test(expected = InterceptorException.class)
    public void testNegativeMaxRedemptionPerUser() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(new Date());
        when(promotion.getEndDate()).thenReturn(futureDate);
        when(promotion.getMaxRedemptionPerUser()).thenReturn(-1);

        interceptor.onValidate(promotion, context);
    }

    @Test(expected = InterceptorException.class)
    public void testMaxPerUserExceedsTotalLimit() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(new Date());
        when(promotion.getEndDate()).thenReturn(futureDate);
        when(promotion.getMaxRedemptionPerUser()).thenReturn(100);
        when(promotion.getTotalRedemptionLimit()).thenReturn(50);

        interceptor.onValidate(promotion, context);
    }

    @Test(expected = InterceptorException.class)
    public void testActiveWithoutDates() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(null);
        when(promotion.getEndDate()).thenReturn(null);
        when(promotion.getStatus()).thenReturn(IS32PromotionStatus.ACTIVE);

        interceptor.onValidate(promotion, context);
    }

    @Test(expected = InterceptorException.class)
    public void testActiveWithoutTag() throws InterceptorException
    {
        when(promotion.getStartDate()).thenReturn(new Date());
        when(promotion.getEndDate()).thenReturn(futureDate);
        when(promotion.getStatus()).thenReturn(IS32PromotionStatus.ACTIVE);
        when(promotion.getPromotionTag()).thenReturn(null);

        interceptor.onValidate(promotion, context);
    }

    @Test
    public void testNewPromotionSkipsStatusValidation() throws InterceptorException
    {
        when(context.isNew(promotion)).thenReturn(true);
        when(promotion.getStartDate()).thenReturn(new Date());
        when(promotion.getEndDate()).thenReturn(futureDate);

        interceptor.onValidate(promotion, context);
    }
}
