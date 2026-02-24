package com.is32.core.dao.impl;

import com.is32.core.dao.IS32PromotionDao;
import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32PromotionTagModel;
import com.is32.core.enums.IS32PromotionStatus;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32PromotionDao implements IS32PromotionDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32PromotionDao.class);

    private static final String FIND_BY_UID =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.UID + "} = ?uid";

    private static final String FIND_BY_STATUS =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.STATUS + "} = ?status";

    private static final String FIND_ACTIVE_PROMOTIONS =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.STATUS + "} = ?status " +
            "AND {p." + IS32PromotionModel.STARTDATE + "} <= ?currentDate " +
            "AND {p." + IS32PromotionModel.ENDDATE + "} > ?currentDate";

    private static final String FIND_ACTIVE_NON_SUSPENDED =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.STATUS + "} = ?status " +
            "AND {p." + IS32PromotionModel.SUSPENDED + "} = ?suspended " +
            "AND {p." + IS32PromotionModel.STARTDATE + "} <= ?currentDate " +
            "AND {p." + IS32PromotionModel.ENDDATE + "} > ?currentDate";

    private static final String FIND_BY_TAG_CODE =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p " +
            "JOIN " + IS32PromotionTagModel._TYPECODE + " AS pt " +
            "ON {p." + IS32PromotionModel.PROMOTIONTAG + "} = {pt." + IS32PromotionTagModel.PK + "}} " +
            "WHERE {pt." + IS32PromotionTagModel.CODE + "} = ?tagCode";

    private static final String FIND_EXPIRED_PROMOTIONS =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.ENDDATE + "} < ?referenceDate " +
            "AND {p." + IS32PromotionModel.STATUS + "} <> ?expiredStatus " +
            "ORDER BY {p." + IS32PromotionModel.ENDDATE + "} ASC";

    private static final String FIND_BY_DATE_RANGE =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p} " +
            "WHERE {p." + IS32PromotionModel.STARTDATE + "} >= ?startDate " +
            "AND {p." + IS32PromotionModel.ENDDATE + "} <= ?endDate " +
            "ORDER BY {p." + IS32PromotionModel.STARTDATE + "} ASC";

    private static final String FIND_FOR_CATALOG_VERSION =
            "SELECT {p." + IS32PromotionModel.PK + "} " +
            "FROM {" + IS32PromotionModel._TYPECODE + " AS p " +
            "JOIN IS32Bucket AS b ON {p." + IS32PromotionModel.UID + "} = {b.promotionUid} " +
            "JOIN IS32PromoItem AS pi ON {pi.bucketUid} = {b.uniqueId} " +
            "JOIN Product AS prod ON {prod.code} = {pi.itemCode}} " +
            "WHERE {prod.catalogVersion} = ?catalogVersion " +
            "AND {p." + IS32PromotionModel.STATUS + "} = ?status " +
            "AND {p." + IS32PromotionModel.STARTDATE + "} <= ?currentDate " +
            "AND {p." + IS32PromotionModel.ENDDATE + "} > ?currentDate";

    private FlexibleSearchService flexibleSearchService;
    private ModelService modelService;

    @Override
    public IS32PromotionModel findByUid(final String uid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_UID, params);
        query.setResultClassList(Collections.singletonList(IS32PromotionModel.class));

        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<IS32PromotionModel> findByStatus(final IS32PromotionStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_STATUS, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findActivePromotions(final Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", IS32PromotionStatus.ACTIVE);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACTIVE_PROMOTIONS, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findActiveNonSuspendedPromotions(final Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", IS32PromotionStatus.ACTIVE);
        params.put("suspended", Boolean.FALSE);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACTIVE_NON_SUSPENDED, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findByPromotionTagCode(final String tagCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("tagCode", tagCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TAG_CODE, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findExpiredPromotions(final Date referenceDate, final int maxResults)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("referenceDate", referenceDate);
        params.put("expiredStatus", IS32PromotionStatus.EXPIRED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_EXPIRED_PROMOTIONS, params);
        query.setCount(maxResults);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findPromotionsByDateRange(final Date startDate, final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_DATE_RANGE, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32PromotionModel> findPromotionsForCatalogVersion(final CatalogVersionModel catalogVersion,
                                                                     final Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("catalogVersion", catalogVersion);
        params.put("status", IS32PromotionStatus.ACTIVE);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_FOR_CATALOG_VERSION, params);
        final SearchResult<IS32PromotionModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
