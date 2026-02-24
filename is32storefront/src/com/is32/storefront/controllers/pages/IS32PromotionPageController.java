package com.is32.storefront.controllers.pages;

import com.is32.facades.dto.AccountQuotaData;
import com.is32.facades.dto.PromotionDisplayData;
import com.is32.facades.facades.IS32PromotionFacade;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractPageController;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/promotions")
public class IS32PromotionPageController extends AbstractPageController
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionPageController.class);

    private static final String PROMOTIONS_CMS_PAGE = "is32PromotionsPage";
    private static final String PROMOTION_DETAIL_CMS_PAGE = "is32PromotionDetailPage";

    @Resource(name = "is32PromotionFacade")
    private IS32PromotionFacade is32PromotionFacade;

    @RequestMapping(method = RequestMethod.GET)
    public String getActivePromotions(final Model model) throws CMSItemNotFoundException
    {
        final List<PromotionDisplayData> promotions = is32PromotionFacade.getActivePromotions();
        model.addAttribute("promotions", promotions);

        storeCmsPageInModel(model, getContentPageForLabelOrId(PROMOTIONS_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROMOTIONS_CMS_PAGE));

        return getViewForPage(model);
    }

    @RequestMapping(value = "/{promotionUid}", method = RequestMethod.GET)
    public String getPromotionDetail(@PathVariable("promotionUid") final String promotionUid,
                                     final Model model) throws CMSItemNotFoundException
    {
        final PromotionDisplayData promotion = is32PromotionFacade.getPromotionWithQuotas(promotionUid);

        if (promotion == null)
        {
            LOG.warn("No promotion found for uid [" + promotionUid + "]");
            return REDIRECT_PREFIX + "/promotions";
        }

        model.addAttribute("promotion", promotion);

        storeCmsPageInModel(model, getContentPageForLabelOrId(PROMOTION_DETAIL_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROMOTION_DETAIL_CMS_PAGE));

        return getViewForPage(model);
    }

    @RequestMapping(value = "/tag/{tagCode}", method = RequestMethod.GET)
    public String getPromotionsByTag(@PathVariable("tagCode") final String tagCode,
                                     final Model model) throws CMSItemNotFoundException
    {
        final List<PromotionDisplayData> promotions = is32PromotionFacade.getPromotionsByTag(tagCode);
        model.addAttribute("promotions", promotions);
        model.addAttribute("selectedTag", tagCode);

        storeCmsPageInModel(model, getContentPageForLabelOrId(PROMOTIONS_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROMOTIONS_CMS_PAGE));

        return getViewForPage(model);
    }

    @RequestMapping(value = "/quotas", method = RequestMethod.GET)
    @ResponseBody
    public List<AccountQuotaData> getAccountQuotas()
    {
        return is32PromotionFacade.getAccountQuotasForCurrentUser();
    }
}
