package com.is32.core.dao;

import de.hybris.platform.core.model.user.CustomerModel;

import java.util.Date;
import java.util.List;

/**
 * DAO for customer synchronization data access operations.
 */
public interface IS32CustomerSyncDao
{
	/**
	 * Finds a customer by their CRM external identifier.
	 *
	 * @param externalId the CRM external ID
	 * @return the customer model or null if not found
	 */
	CustomerModel findCustomerByExternalId(String externalId);

	/**
	 * Retrieves customers modified since the given date.
	 *
	 * @param sinceDate the date threshold
	 * @param maxResults maximum results to return
	 * @return list of modified customers
	 */
	List<CustomerModel> findCustomersModifiedSince(Date sinceDate, int maxResults);

	/**
	 * Searches customers by a dynamic filter expression.
	 *
	 * @param filterExpression the filter expression
	 * @param maxResults       maximum results
	 * @return list of matching customers
	 */
	List<CustomerModel> searchCustomersByFilter(String filterExpression, int maxResults);

	/**
	 * Removes sync tracking records older than the specified date.
	 *
	 * @param beforeDate the date threshold
	 * @return number of records removed
	 */
	int removeSyncRecordsBefore(Date beforeDate);

	/**
	 * Retrieves the last sync timestamp for a customer.
	 *
	 * @param customerId the customer ID
	 * @return timestamp in milliseconds or -1
	 */
	long getLastSyncTimestamp(String customerId);
}
