package com.is32.core.dao;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.catalog.model.CatalogVersionModel;

import java.util.Date;
import java.util.List;

public interface IS32PromotionDao
{
    IS32PromotionModel findByUid(String uid);

    List<IS32PromotionModel> findByStatus(IS32PromotionStatus status);

    List<IS32PromotionModel> findActivePromotions(Date currentDate);

    List<IS32PromotionModel> findActiveNonSuspendedPromotions(Date currentDate);

    List<IS32PromotionModel> findByPromotionTagCode(String tagCode);

    List<IS32PromotionModel> findExpiredPromotions(Date referenceDate, int maxResults);

    List<IS32PromotionModel> findPromotionsByDateRange(Date startDate, Date endDate);

    List<IS32PromotionModel> findPromotionsForCatalogVersion(CatalogVersionModel catalogVersion, Date currentDate);
}
