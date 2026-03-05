package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32CustomerReviewDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32CustomerReviewDao.class);

    // FSQL-02 violation: aggregate function (avg) without pagination or result bounding
    private static final String GET_AVERAGE_RATING =
            "SELECT avg({cr.rating}) FROM {CustomerReview AS cr} " +
            "WHERE {cr.product} = ?product " +
            "AND ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {cr.approvalStatus} != ?rejectedStatus";

    // FSQL-02 violation: another aggregate (COUNT) without bounding
    private static final String COUNT_REVIEWS_BY_PRODUCT =
            "SELECT COUNT({cr.pk}) FROM {CustomerReview AS cr} " +
            "WHERE {cr.product} = ?product " +
            "AND {cr.approvalStatus} = ?approvedStatus";

    // FSQL-02 violation: SUM aggregate without pagination
    private static final String SUM_RATINGS_BY_STORE =
            "SELECT SUM({cr.rating}) FROM {CustomerReview AS cr JOIN BaseStore AS bs ON {cr.baseStore} = {bs.pk}} " +
            "WHERE {bs.uid} = ?storeUid " +
            "AND {cr.blocked} = ?blocked";

    private FlexibleSearchService flexibleSearchService;

    public Double getAverageRating(final Object product)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("product", product);
        params.put("blocked", Boolean.FALSE);
        params.put("rejectedStatus", "REJECTED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_AVERAGE_RATING, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty() && rows.get(0) != null)
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return null;
    }

    public Long countReviewsByProduct(final Object product)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("product", product);
        params.put("approvedStatus", "APPROVED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(COUNT_REVIEWS_BY_PRODUCT, params);
        final SearchResult<List<Long>> result = flexibleSearchService.search(query);
        final List<List<Long>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Long) ((List) rows.get(0)).get(0);
        }
        return 0L;
    }

    public Double sumRatingsByStore(final String storeUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("storeUid", storeUid);
        params.put("blocked", Boolean.FALSE);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(SUM_RATINGS_BY_STORE, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return 0.0;
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
