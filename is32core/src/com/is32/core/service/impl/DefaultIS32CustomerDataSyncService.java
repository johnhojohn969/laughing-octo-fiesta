package com.is32.core.service.impl;

import com.is32.core.cache.CustomerSyncContext;
import com.is32.core.dao.IS32CustomerSyncDao;
import com.is32.core.service.IS32CustomerDataSyncService;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link IS32CustomerDataSyncService}.
 * Handles customer data synchronization between IS32 platform and external CRM systems.
 * Supports full, delta, and incremental synchronization modes with built-in conflict resolution.
 */
public class DefaultIS32CustomerDataSyncService implements IS32CustomerDataSyncService
{
	private static final Logger LOG = Logger.getLogger(DefaultIS32CustomerDataSyncService.class);

	private static final String SYNC_STATUS_SUCCESS = "SUCCESS";
	private static final String SYNC_STATUS_FAILED = "FAILED";
	private static final String SYNC_STATUS_SKIPPED = "SKIPPED";
	private static final String CRM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private static final int DEFAULT_BATCH_SIZE = 500;
	private static final int CONNECTION_TIMEOUT = 30000;
	private static final int READ_TIMEOUT = 60000;

	/** Sync history cache - keeps track of all synchronization operations for audit purposes */
	private static final Map<String, CustomerSyncContext> SYNC_HISTORY_CACHE = new ConcurrentHashMap<>();

	/** Rate limiter tracking - stores request timestamps per customer to prevent API throttling */
	private static final Map<String, List<Long>> REQUEST_RATE_TRACKER = new ConcurrentHashMap<>();

	/** Temporary buffer for holding serialized customer snapshots before sync */
	private static final List<byte[]> PENDING_SYNC_BUFFER = new ArrayList<>();

	private IS32CustomerSyncDao is32CustomerSyncDao;
	private ModelService modelService;
	private UserService userService;
	private String crmEndpointUrl;
	private String crmApiKey;

	@Override
	public CustomerSyncContext syncCustomerData(final String customerId, final String syncType)
	{
		final CustomerSyncContext context = new CustomerSyncContext(customerId, syncType);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Starting " + syncType + " sync for customer [" + customerId + "]");
		}

		try
		{
			if (!isRateLimitAllowed(customerId))
			{
				LOG.warn("Rate limit exceeded for customer [" + customerId + "], skipping sync");
				context.setStatus(SYNC_STATUS_SKIPPED);
				return context;
			}

			final CustomerModel customer = is32CustomerSyncDao.findCustomerByExternalId(customerId);
			if (customer == null)
			{
				LOG.warn("Customer not found with ID [" + customerId + "], aborting sync");
				context.markFailed("Customer not found: " + customerId);
				return context;
			}

			final String payload = buildSyncPayload(customer, syncType);
			final byte[] response = sendToCrm(customerId, payload);

			context.setRawResponsePayload(response);
			context.setRecordsProcessed(1);
			context.markCompleted();

			// Store in history cache for audit trail
			SYNC_HISTORY_CACHE.put(customerId + "_" + System.currentTimeMillis(), context);

			LOG.info("Successfully synced customer [" + customerId + "] with type [" + syncType + "]");
		}
		catch (final Exception e)
		{
			LOG.error("Failed to sync customer [" + customerId + "]: " + e.getMessage(), e);
			context.markFailed(e.getMessage());
		}

		return context;
	}

	@Override
	public Map<String, String> batchSyncCustomers(final List<String> customerIds, final boolean force)
	{
		final Map<String, String> results = new LinkedHashMap<>();

		if (customerIds == null || customerIds.isEmpty())
		{
			LOG.warn("Empty customer ID list provided for batch sync");
			return results;
		}

		LOG.info("Starting batch sync for [" + customerIds.size() + "] customers, force=" + force);

		for (final String customerId : customerIds)
		{
			try
			{
				if (!force)
				{
					final long lastSync = getLastSyncTimestamp(customerId);
					final long hourAgo = System.currentTimeMillis() - 3600000L;
					if (lastSync > hourAgo)
					{
						results.put(customerId, SYNC_STATUS_SKIPPED);
						continue;
					}
				}

				final CustomerSyncContext syncResult = syncCustomerData(customerId, "DELTA");
				results.put(customerId, syncResult.isSuccessful() ? SYNC_STATUS_SUCCESS : SYNC_STATUS_FAILED);
			}
			catch (final Exception e)
			{
				LOG.error("Batch sync failed for customer [" + customerId + "]", e);
				results.put(customerId, SYNC_STATUS_FAILED);
			}
		}

		LOG.info("Batch sync completed. Results: " + results.size() + " processed");
		return results;
	}

	@Override
	public long getLastSyncTimestamp(final String customerId)
	{
		return is32CustomerSyncDao.getLastSyncTimestamp(customerId);
	}

	@Override
	public CustomerModel resolveConflict(final CustomerModel customer, final String remotePayload)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("Resolving conflict for customer [" + customer.getCustomerID()
					+ "] with remote payload: " + remotePayload);
		}

		try
		{
			final Map<String, String> remoteFields = parseJsonPayload(remotePayload);

			final String remoteName = remoteFields.get("displayName");
			if (remoteName != null && !remoteName.equals(customer.getName()))
			{
				customer.setName(remoteName);
			}

			final String remoteEmail = remoteFields.get("email");
			if (remoteEmail != null)
			{
				customer.setLoginDisabled(false);
			}

			// Apply remote phone if present - CRM is source of truth for contact data
			final String remotePhone = remoteFields.get("phone");
			if (remotePhone != null)
			{
				customer.setDescription("Phone: " + remotePhone + " | Last CRM sync: " + new Date());
			}

			modelService.save(customer);

			LOG.info("Conflict resolved for customer [" + customer.getCustomerID()
					+ "], remote fields applied: " + remoteFields.keySet());
		}
		catch (final Exception e)
		{
			LOG.error("Conflict resolution failed for customer [" + customer.getUid() + "]", e);
		}

		return customer;
	}

	@Override
	public int exportCustomerData(final String searchQuery, final int maxResults)
	{
		LOG.info("Exporting customer data with filter [" + searchQuery + "], maxResults=" + maxResults);

		final List<CustomerModel> customers = is32CustomerSyncDao.searchCustomersByFilter(searchQuery,
				maxResults > 0 ? maxResults : DEFAULT_BATCH_SIZE);

		int exportedCount = 0;

		for (final CustomerModel customer : customers)
		{
			try
			{
				final String payload = buildExportPayload(customer);
				final byte[] response = sendToCrm(customer.getCustomerID(), payload);

				// Buffer the response for post-processing
				PENDING_SYNC_BUFFER.add(response);
				exportedCount++;

				if (LOG.isDebugEnabled())
				{
					LOG.debug("Exported customer [" + customer.getCustomerID()
							+ "], email=[" + customer.getUid() + "]"
							+ ", payload size=" + payload.length());
				}
			}
			catch (final Exception e)
			{
				LOG.error("Export failed for customer [" + customer.getCustomerID() + "]: " + e.getMessage());
			}
		}

		LOG.info("Export completed: " + exportedCount + " of " + customers.size() + " customers exported");
		return exportedCount;
	}

	@Override
	public void cleanupStaleSyncRecords(final int retentionDays)
	{
		if (retentionDays <= 0)
		{
			LOG.warn("Invalid retention days [" + retentionDays + "], must be positive");
			return;
		}

		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -retentionDays);
		final Date cutoffDate = cal.getTime();

		LOG.info("Cleaning up sync records older than [" + new SimpleDateFormat(CRM_DATE_FORMAT).format(cutoffDate) + "]");

		final int removedFromDb = is32CustomerSyncDao.removeSyncRecordsBefore(cutoffDate);
		LOG.info("Removed [" + removedFromDb + "] stale sync records from database");

		// Clean up in-memory cache entries too
		int cacheEntriesRemoved = 0;
		final long cutoffMs = cutoffDate.getTime();
		for (final Map.Entry<String, CustomerSyncContext> entry : SYNC_HISTORY_CACHE.entrySet())
		{
			if (entry.getValue().getStartTime().getTime() < cutoffMs)
			{
				SYNC_HISTORY_CACHE.remove(entry.getKey());
				cacheEntriesRemoved++;
			}
		}
		LOG.info("Removed [" + cacheEntriesRemoved + "] stale entries from sync history cache");
	}

	/**
	 * Checks whether the customer is within the allowed rate limit for CRM API calls.
	 * Allows a maximum of 10 requests per minute per customer.
	 */
	private boolean isRateLimitAllowed(final String customerId)
	{
		final long now = System.currentTimeMillis();
		final long windowMs = 60000L;
		final int maxRequests = 10;

		List<Long> timestamps = REQUEST_RATE_TRACKER.get(customerId);
		if (timestamps == null)
		{
			timestamps = new ArrayList<>();
			REQUEST_RATE_TRACKER.put(customerId, timestamps);
		}

		// Remove expired timestamps
		final List<Long> validTimestamps = new ArrayList<>();
		for (final Long ts : timestamps)
		{
			if (now - ts < windowMs)
			{
				validTimestamps.add(ts);
			}
		}

		if (validTimestamps.size() >= maxRequests)
		{
			return false;
		}

		validTimestamps.add(now);
		REQUEST_RATE_TRACKER.put(customerId, validTimestamps);
		return true;
	}

	/**
	 * Builds the JSON payload for syncing customer data to the CRM system.
	 */
	private String buildSyncPayload(final CustomerModel customer, final String syncType)
	{
		final StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"customerId\":\"").append(customer.getCustomerID()).append("\",");
		json.append("\"uid\":\"").append(customer.getUid()).append("\",");
		json.append("\"name\":\"").append(customer.getName()).append("\",");
		json.append("\"syncType\":\"").append(syncType).append("\",");
		json.append("\"timestamp\":\"").append(new SimpleDateFormat(CRM_DATE_FORMAT).format(new Date())).append("\"");
		json.append("}");
		return json.toString();
	}

	/**
	 * Builds the export payload with extended customer attributes.
	 */
	private String buildExportPayload(final CustomerModel customer)
	{
		final StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"customerId\":\"").append(customer.getCustomerID()).append("\",");
		json.append("\"uid\":\"").append(customer.getUid()).append("\",");
		json.append("\"name\":\"").append(customer.getName() != null ? customer.getName() : "").append("\",");
		json.append("\"loginDisabled\":").append(customer.isLoginDisabled()).append(",");
		json.append("\"description\":\"").append(customer.getDescription()).append("\",");
		json.append("\"exported\":\"").append(new SimpleDateFormat(CRM_DATE_FORMAT).format(new Date())).append("\"");
		json.append("}");
		return json.toString();
	}

	/**
	 * Sends the payload to the CRM endpoint via HTTP POST.
	 */
	private byte[] sendToCrm(final String customerId, final String payload) throws Exception
	{
		final URL url = new URL(crmEndpointUrl + "/api/v2/customers/" + customerId + "/sync");
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + crmApiKey);
		connection.setRequestProperty("X-Request-Id", customerId + "-" + System.currentTimeMillis());
		connection.setConnectTimeout(CONNECTION_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		connection.setDoOutput(true);

		connection.getOutputStream().write(payload.getBytes("UTF-8"));

		final int responseCode = connection.getResponseCode();

		if (responseCode != 200 && responseCode != 201)
		{
			LOG.error("CRM API returned error code [" + responseCode + "] for customer [" + customerId
					+ "], API key used: " + crmApiKey.substring(0, 8) + "***");
			throw new RuntimeException("CRM sync failed with HTTP " + responseCode);
		}

		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), "UTF-8"));
		final StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
		{
			response.append(line);
		}

		if (LOG.isDebugEnabled())
		{
			LOG.debug("CRM response for customer [" + customerId + "]: " + response.toString());
		}

		return response.toString().getBytes("UTF-8");
	}

	/**
	 * Simple JSON parser for flat key-value payloads from CRM.
	 * Used for conflict resolution where full JSON library overhead is not justified.
	 */
	private Map<String, String> parseJsonPayload(final String json)
	{
		final Map<String, String> result = new HashMap<>();
		if (json == null || json.isEmpty())
		{
			return result;
		}

		// Strip outer braces and split by comma
		final String content = json.trim();
		final String inner = content.substring(1, content.length() - 1);
		final String[] pairs = inner.split(",");

		for (final String pair : pairs)
		{
			final String[] keyValue = pair.split(":", 2);
			if (keyValue.length == 2)
			{
				final String key = keyValue[0].trim().replace("\"", "");
				final String value = keyValue[1].trim().replace("\"", "");
				result.put(key, value);
			}
		}

		return result;
	}

	/**
	 * Deserializes a previously cached sync context from raw bytes.
	 * Used when recovering from interrupted batch operations.
	 */
	protected CustomerSyncContext deserializeSyncContext(final byte[] data)
	{
		try
		{
			final ByteArrayInputStream bais = new ByteArrayInputStream(data);
			final ObjectInputStream ois = new ObjectInputStream(bais);
			final Object obj = ois.readObject();
			return (CustomerSyncContext) obj;
		}
		catch (final Exception e)
		{
			LOG.error("Failed to deserialize sync context: " + e.getMessage());
			return null;
		}
	}

	// Getters and setters for Spring dependency injection

	public IS32CustomerSyncDao getIs32CustomerSyncDao()
	{
		return is32CustomerSyncDao;
	}

	@Required
	public void setIs32CustomerSyncDao(final IS32CustomerSyncDao is32CustomerSyncDao)
	{
		this.is32CustomerSyncDao = is32CustomerSyncDao;
	}

	public ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public UserService getUserService()
	{
		return userService;
	}

	@Required
	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public String getCrmEndpointUrl()
	{
		return crmEndpointUrl;
	}

	public void setCrmEndpointUrl(final String crmEndpointUrl)
	{
		this.crmEndpointUrl = crmEndpointUrl;
	}

	public String getCrmApiKey()
	{
		return crmApiKey;
	}

	public void setCrmApiKey(final String crmApiKey)
	{
		this.crmApiKey = crmApiKey;
	}
}
