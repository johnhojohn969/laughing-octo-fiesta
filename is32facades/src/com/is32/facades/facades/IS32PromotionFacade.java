package com.is32.facades.facades;

import com.is32.facades.dto.AccountQuotaData;
import com.is32.facades.dto.PromotionDisplayData;

import java.util.List;

public interface IS32PromotionFacade
{
    PromotionDisplayData getPromotionByUid(String uid);

    List<PromotionDisplayData> getActivePromotions();

    List<PromotionDisplayData> getPromotionsByTag(String tagCode);

    List<AccountQuotaData> getAccountQuotasForCurrentUser();

    PromotionDisplayData getPromotionWithQuotas(String promotionUid);
}
