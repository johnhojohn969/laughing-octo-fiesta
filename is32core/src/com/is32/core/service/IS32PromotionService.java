package com.is32.core.service;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.catalog.model.CatalogVersionModel;

import java.util.Date;
import java.util.List;

public interface IS32PromotionService
{
    IS32PromotionModel getPromotionByUid(String uid);

    List<IS32PromotionModel> getPromotionsByStatus(IS32PromotionStatus status);

    List<IS32PromotionModel> getActivePromotions();

    List<IS32PromotionModel> getActiveNonSuspendedPromotions();

    List<IS32PromotionModel> getPromotionsByTagCode(String tagCode);

    List<IS32PromotionModel> getExpiredPromotions(int maxResults);

    List<IS32PromotionModel> getPromotionsByDateRange(Date startDate, Date endDate);

    List<IS32PromotionModel> getPromotionsForCatalogVersion(CatalogVersionModel catalogVersion);

    void updatePromotionStatus(IS32PromotionModel promotion, IS32PromotionStatus newStatus);

    void suspendPromotion(IS32PromotionModel promotion);

    void resumePromotion(IS32PromotionModel promotion);

    void markExpiredPromotions();
}
