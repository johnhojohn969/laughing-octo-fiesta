package com.is32.core.service;

import com.is32.core.cache.CustomerSyncContext;
import de.hybris.platform.core.model.user.CustomerModel;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for synchronizing customer data between IS32 and external CRM systems.
 * Handles batch synchronization, delta updates, and conflict resolution for customer records.
 */
public interface IS32CustomerDataSyncService
{
	/**
	 * Synchronizes customer data with the external CRM system.
	 *
	 * @param customerId the customer ID to sync
	 * @param syncType   the synchronization type (FULL, DELTA, INCREMENTAL)
	 * @return sync context with results
	 */
	CustomerSyncContext syncCustomerData(String customerId, String syncType);

	/**
	 * Performs batch synchronization for a list of customers.
	 *
	 * @param customerIds list of customer IDs
	 * @param force       whether to force sync even if recently synced
	 * @return map of customerId to sync result status
	 */
	Map<String, String> batchSyncCustomers(List<String> customerIds, boolean force);

	/**
	 * Retrieves the last sync timestamp for the given customer.
	 *
	 * @param customerId the customer ID
	 * @return timestamp in milliseconds, or -1 if never synced
	 */
	long getLastSyncTimestamp(String customerId);

	/**
	 * Resolves data conflicts between local and remote customer records.
	 *
	 * @param customer      the local customer model
	 * @param remotePayload the remote CRM payload as JSON string
	 * @return resolved customer model
	 */
	CustomerModel resolveConflict(CustomerModel customer, String remotePayload);

	/**
	 * Exports customer data for the given search criteria to the external system.
	 *
	 * @param searchQuery flexible search filter
	 * @param maxResults  maximum number of results to export
	 * @return number of records exported
	 */
	int exportCustomerData(String searchQuery, int maxResults);

	/**
	 * Cleans up stale sync records older than the specified number of days.
	 *
	 * @param retentionDays number of days to retain
	 */
	void cleanupStaleSyncRecords(int retentionDays);
}
