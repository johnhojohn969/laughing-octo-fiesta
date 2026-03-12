package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo DAO for Gemini SQL-02 pattern detection.
 * All queries in this class structurally match the SQL-02 pattern from styleguide.md Section 5.1:
 * aggregate queries on CustomerReview with filters on product, blocked, and approvalStatus.
 */
public class DefaultIS32ProductRatingDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32ProductRatingDao.class);

    // SQL-02 match: AVG aggregate on CustomerReview filtered by product + blocked + approvalStatus
    private static final String AVG_RATING_BY_CATEGORY =
            "SELECT avg({cr.rating}) FROM {CustomerReview AS cr JOIN Category2ProductRelation AS rel ON {cr.product} = {rel.target}} " +
            "WHERE {rel.source} = ?category " +
            "AND ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {cr.approvalStatus} != ?rejectedStatus";

    // SQL-02 match: COUNT aggregate on CustomerReview filtered by product + approvalStatus
    private static final String COUNT_APPROVED_REVIEWS_BY_DATE =
            "SELECT COUNT({cr.pk}) FROM {CustomerReview AS cr} " +
            "WHERE {cr.product} = ?product " +
            "AND {cr.approvalStatus} = ?approvedStatus " +
            "AND {cr.creationtime} >= ?startDate";

    // SQL-02 match: MIN/MAX aggregates on CustomerReview with blocked + approvalStatus filters
    private static final String MIN_MAX_RATING =
            "SELECT MIN({cr.rating}), MAX({cr.rating}) FROM {CustomerReview AS cr} " +
            "WHERE {cr.product} = ?product " +
            "AND ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {cr.approvalStatus} != ?rejectedStatus";

    // SQL-02 match: AVG with GROUP BY — still matches the core fingerprint
    private static final String AVG_RATING_PER_PRODUCT =
            "SELECT {cr.product}, avg({cr.rating}) FROM {CustomerReview AS cr} " +
            "WHERE ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {cr.approvalStatus} != ?rejectedStatus " +
            "GROUP BY {cr.product}";

    private FlexibleSearchService flexibleSearchService;

    public Double getAverageRatingByCategory(final Object category)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("category", category);
        params.put("blocked", Boolean.FALSE);
        params.put("rejectedStatus", "REJECTED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(AVG_RATING_BY_CATEGORY, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty() && rows.get(0) != null)
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return null;
    }

    public Long countApprovedReviewsByDate(final Object product, final Object startDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("product", product);
        params.put("approvedStatus", "APPROVED");
        params.put("startDate", startDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(COUNT_APPROVED_REVIEWS_BY_DATE, params);
        final SearchResult<List<Long>> result = flexibleSearchService.search(query);
        final List<List<Long>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Long) ((List) rows.get(0)).get(0);
        }
        return 0L;
    }

    public List<Object> getMinMaxRating(final Object product)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("product", product);
        params.put("blocked", Boolean.FALSE);
        params.put("rejectedStatus", "REJECTED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(MIN_MAX_RATING, params);
        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        final List<List<Object>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return rows.get(0);
        }
        return null;
    }

    public List<Object> getAverageRatingPerProduct()
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("blocked", Boolean.FALSE);
        params.put("rejectedStatus", "REJECTED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(AVG_RATING_PER_PRODUCT, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
