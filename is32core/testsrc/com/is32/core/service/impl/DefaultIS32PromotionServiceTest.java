package com.is32.core.service.impl;

import com.is32.core.dao.IS32AccountQuotaDao;
import com.is32.core.dao.IS32PromotionDao;
import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultIS32PromotionServiceTest
{
    private static final String PROMOTION_UID = "PROMO_001";

    @InjectMocks
    private DefaultIS32PromotionService is32PromotionService;

    @Mock
    private IS32PromotionDao is32PromotionDao;

    @Mock
    private IS32AccountQuotaDao is32AccountQuotaDao;

    @Mock
    private ModelService modelService;

    @Mock
    private IS32PromotionModel promotionModel;

    @Before
    public void setUp()
    {
        when(promotionModel.getUid()).thenReturn(PROMOTION_UID);
        when(promotionModel.getStatus()).thenReturn(IS32PromotionStatus.ACTIVE);
    }

    @Test
    public void testGetPromotionByUid()
    {
        when(is32PromotionDao.findByUid(PROMOTION_UID)).thenReturn(promotionModel);

        final IS32PromotionModel result = is32PromotionService.getPromotionByUid(PROMOTION_UID);

        assertNotNull(result);
        assertEquals(PROMOTION_UID, result.getUid());
        verify(is32PromotionDao).findByUid(PROMOTION_UID);
    }

    @Test
    public void testGetPromotionByUidReturnsNullForUnknown()
    {
        when(is32PromotionDao.findByUid("UNKNOWN")).thenReturn(null);

        final IS32PromotionModel result = is32PromotionService.getPromotionByUid("UNKNOWN");

        assertNull(result);
    }

    @Test
    public void testGetPromotionsByStatus()
    {
        when(is32PromotionDao.findByStatus(IS32PromotionStatus.ACTIVE))
                .thenReturn(Arrays.asList(promotionModel));

        final List<IS32PromotionModel> result =
                is32PromotionService.getPromotionsByStatus(IS32PromotionStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetActivePromotions()
    {
        when(is32PromotionDao.findActivePromotions(any(Date.class)))
                .thenReturn(Arrays.asList(promotionModel));

        final List<IS32PromotionModel> result = is32PromotionService.getActivePromotions();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testUpdatePromotionStatus()
    {
        is32PromotionService.updatePromotionStatus(promotionModel, IS32PromotionStatus.INACTIVE);

        verify(promotionModel).setStatus(IS32PromotionStatus.INACTIVE);
        verify(modelService).save(promotionModel);
    }

    @Test
    public void testSuspendPromotion()
    {
        is32PromotionService.suspendPromotion(promotionModel);

        verify(promotionModel).setSuspended(Boolean.TRUE);
        verify(modelService).save(promotionModel);
    }

    @Test
    public void testResumePromotion()
    {
        is32PromotionService.resumePromotion(promotionModel);

        verify(promotionModel).setSuspended(Boolean.FALSE);
        verify(modelService).save(promotionModel);
    }

    @Test
    public void testMarkExpiredPromotions()
    {
        final IS32PromotionModel expiredPromo = org.mockito.Mockito.mock(IS32PromotionModel.class);
        when(expiredPromo.getStatus()).thenReturn(IS32PromotionStatus.ACTIVE);

        when(is32PromotionDao.findExpiredPromotions(any(Date.class), eq(Integer.MAX_VALUE)))
                .thenReturn(Collections.singletonList(expiredPromo));

        is32PromotionService.markExpiredPromotions();

        verify(expiredPromo).setStatus(IS32PromotionStatus.EXPIRED);
        verify(modelService).save(expiredPromo);
    }

    @Test
    public void testMarkExpiredPromotionsSkipsAlreadyExpired()
    {
        final IS32PromotionModel alreadyExpired = org.mockito.Mockito.mock(IS32PromotionModel.class);
        when(alreadyExpired.getStatus()).thenReturn(IS32PromotionStatus.EXPIRED);

        when(is32PromotionDao.findExpiredPromotions(any(Date.class), eq(Integer.MAX_VALUE)))
                .thenReturn(Collections.singletonList(alreadyExpired));

        is32PromotionService.markExpiredPromotions();

        verify(alreadyExpired, never()).setStatus(any());
        verify(modelService, never()).save(alreadyExpired);
    }

    @Test
    public void testGetExpiredPromotions()
    {
        when(is32PromotionDao.findExpiredPromotions(any(Date.class), eq(50)))
                .thenReturn(Collections.emptyList());

        final List<IS32PromotionModel> result = is32PromotionService.getExpiredPromotions(50);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
