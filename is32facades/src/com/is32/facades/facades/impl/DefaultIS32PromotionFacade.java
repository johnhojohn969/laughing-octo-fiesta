package com.is32.facades.facades.impl;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.service.IS32AccountQuotaService;
import com.is32.core.service.IS32PromotionService;
import com.is32.facades.dto.AccountQuotaData;
import com.is32.facades.dto.PromotionDisplayData;
import com.is32.facades.facades.IS32PromotionFacade;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultIS32PromotionFacade implements IS32PromotionFacade
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionFacade.class);

    private IS32PromotionService is32PromotionService;
    private IS32AccountQuotaService is32AccountQuotaService;
    private Converter<IS32PromotionModel, PromotionDisplayData> is32PromotionConverter;
    private Converter<Map<String, Object>, AccountQuotaData> accountQuotaConverter;
    private UserService userService;

    @Override
    public PromotionDisplayData getPromotionByUid(final String uid)
    {
        final IS32PromotionModel promotion = is32PromotionService.getPromotionByUid(uid);
        if (promotion == null)
        {
            return null;
        }
        return is32PromotionConverter.convert(promotion);
    }

    @Override
    public List<PromotionDisplayData> getActivePromotions()
    {
        final List<IS32PromotionModel> activePromotions = is32PromotionService.getActiveNonSuspendedPromotions();
        return activePromotions.stream()
                .map(is32PromotionConverter::convert)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionDisplayData> getPromotionsByTag(final String tagCode)
    {
        final List<IS32PromotionModel> promotions = is32PromotionService.getPromotionsByTagCode(tagCode);
        return promotions.stream()
                .map(is32PromotionConverter::convert)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountQuotaData> getAccountQuotasForCurrentUser()
    {
        final List<Map<String, Object>> rawQuotas =
                is32AccountQuotaService.getAccountQuotasForCurrentUser();

        final List<AccountQuotaData> result = new ArrayList<>();
        for (final Map<String, Object> quota : rawQuotas)
        {
            result.add(accountQuotaConverter.convert(quota));
        }
        return result;
    }

    @Override
    public PromotionDisplayData getPromotionWithQuotas(final String promotionUid)
    {
        final PromotionDisplayData promotionData = getPromotionByUid(promotionUid);
        if (promotionData == null)
        {
            return null;
        }

        final List<AccountQuotaData> quotas = getAccountQuotasForCurrentUser();
        promotionData.setAccountQuotas(quotas);

        return promotionData;
    }

    @Required
    public void setIs32PromotionService(final IS32PromotionService is32PromotionService)
    {
        this.is32PromotionService = is32PromotionService;
    }

    @Required
    public void setIs32AccountQuotaService(final IS32AccountQuotaService is32AccountQuotaService)
    {
        this.is32AccountQuotaService = is32AccountQuotaService;
    }

    @Required
    public void setIs32PromotionConverter(
            final Converter<IS32PromotionModel, PromotionDisplayData> is32PromotionConverter)
    {
        this.is32PromotionConverter = is32PromotionConverter;
    }

    @Required
    public void setAccountQuotaConverter(
            final Converter<Map<String, Object>, AccountQuotaData> accountQuotaConverter)
    {
        this.accountQuotaConverter = accountQuotaConverter;
    }

    @Required
    public void setUserService(final UserService userService)
    {
        this.userService = userService;
    }
}
