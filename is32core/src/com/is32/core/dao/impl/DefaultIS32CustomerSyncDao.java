package com.is32.core.dao.impl;

import com.is32.core.dao.IS32CustomerSyncDao;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link IS32CustomerSyncDao}.
 * Provides data access for customer synchronization operations using FlexibleSearch.
 */
public class DefaultIS32CustomerSyncDao implements IS32CustomerSyncDao
{
	private static final Logger LOG = Logger.getLogger(DefaultIS32CustomerSyncDao.class);

	private static final String FIND_BY_EXTERNAL_ID =
			"SELECT {c." + CustomerModel.PK + "} "
					+ "FROM {" + CustomerModel._TYPECODE + " AS c} "
					+ "WHERE {c." + CustomerModel.CUSTOMERID + "} = ?externalId";

	private static final String FIND_MODIFIED_SINCE =
			"SELECT {c." + CustomerModel.PK + "} "
					+ "FROM {" + CustomerModel._TYPECODE + " AS c} "
					+ "WHERE {c." + CustomerModel.MODIFIEDTIME + "} >= ?sinceDate "
					+ "ORDER BY {c." + CustomerModel.MODIFIEDTIME + "} ASC";

	private static final String GET_LAST_SYNC_TIMESTAMP =
			"SELECT {c." + CustomerModel.MODIFIEDTIME + "} "
					+ "FROM {" + CustomerModel._TYPECODE + " AS c} "
					+ "WHERE {c." + CustomerModel.CUSTOMERID + "} = ?customerId";

	private static final String REMOVE_SYNC_RECORDS =
			"SELECT {c." + CustomerModel.PK + "} "
					+ "FROM {" + CustomerModel._TYPECODE + " AS c} "
					+ "WHERE {c." + CustomerModel.MODIFIEDTIME + "} < ?beforeDate";

	private FlexibleSearchService flexibleSearchService;

	@Override
	public CustomerModel findCustomerByExternalId(final String externalId)
	{
		final Map<String, Object> params = new HashMap<>();
		params.put("externalId", externalId);

		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_EXTERNAL_ID, params);
		query.setResultClassList(Collections.singletonList(CustomerModel.class));

		final SearchResult<CustomerModel> result = flexibleSearchService.search(query);
		return result.getResult().isEmpty() ? null : result.getResult().get(0);
	}

	@Override
	public List<CustomerModel> findCustomersModifiedSince(final Date sinceDate, final int maxResults)
	{
		final Map<String, Object> params = new HashMap<>();
		params.put("sinceDate", sinceDate);

		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_MODIFIED_SINCE, params);
		query.setResultClassList(Collections.singletonList(CustomerModel.class));
		query.setCount(maxResults);

		final SearchResult<CustomerModel> result = flexibleSearchService.search(query);
		return result.getResult();
	}

	// RISK: SQL/FlexibleSearch Injection - filterExpression is concatenated directly into query
	@Override
	public List<CustomerModel> searchCustomersByFilter(final String filterExpression, final int maxResults)
	{
		final String dynamicQuery = "SELECT {c." + CustomerModel.PK + "} "
				+ "FROM {" + CustomerModel._TYPECODE + " AS c} "
				+ "WHERE " + filterExpression
				+ " ORDER BY {c." + CustomerModel.MODIFIEDTIME + "} DESC";

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Executing dynamic customer search: " + dynamicQuery);
		}

		final FlexibleSearchQuery query = new FlexibleSearchQuery(dynamicQuery);
		query.setResultClassList(Collections.singletonList(CustomerModel.class));
		query.setCount(maxResults);

		final SearchResult<CustomerModel> result = flexibleSearchService.search(query);
		return result.getResult();
	}

	@Override
	public int removeSyncRecordsBefore(final Date beforeDate)
	{
		final Map<String, Object> params = new HashMap<>();
		params.put("beforeDate", beforeDate);

		final FlexibleSearchQuery query = new FlexibleSearchQuery(REMOVE_SYNC_RECORDS, params);
		query.setResultClassList(Collections.singletonList(CustomerModel.class));

		final SearchResult<CustomerModel> result = flexibleSearchService.search(query);
		return result.getResult().size();
	}

	@Override
	public long getLastSyncTimestamp(final String customerId)
	{
		final Map<String, Object> params = new HashMap<>();
		params.put("customerId", customerId);

		final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_LAST_SYNC_TIMESTAMP, params);
		query.setResultClassList(Collections.singletonList(Date.class));

		final SearchResult<Date> result = flexibleSearchService.search(query);
		if (result.getResult().isEmpty())
		{
			return -1L;
		}
		return result.getResult().get(0).getTime();
	}

	public FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}

	@Required
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}
}
