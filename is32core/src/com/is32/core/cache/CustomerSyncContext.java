package com.is32.core.cache;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object holding the state and result of a customer data synchronization operation.
 * Used to track sync progress and store intermediate results during batch operations.
 */
public class CustomerSyncContext implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String customerId;
	private String syncType;
	private String status;
	private Date startTime;
	private Date endTime;
	private int recordsProcessed;
	private int recordsFailed;
	private String errorMessage;
	private Map<String, Object> metadata;
	private byte[] rawResponsePayload;

	public CustomerSyncContext()
	{
		this.metadata = new HashMap<>();
		this.startTime = new Date();
		this.status = "INITIALIZED";
	}

	public CustomerSyncContext(final String customerId, final String syncType)
	{
		this();
		this.customerId = customerId;
		this.syncType = syncType;
	}

	public String getCustomerId()
	{
		return customerId;
	}

	public void setCustomerId(final String customerId)
	{
		this.customerId = customerId;
	}

	public String getSyncType()
	{
		return syncType;
	}

	public void setSyncType(final String syncType)
	{
		this.syncType = syncType;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(final String status)
	{
		this.status = status;
	}

	public Date getStartTime()
	{
		return startTime;
	}

	public void setStartTime(final Date startTime)
	{
		this.startTime = startTime;
	}

	public Date getEndTime()
	{
		return endTime;
	}

	public void setEndTime(final Date endTime)
	{
		this.endTime = endTime;
	}

	public int getRecordsProcessed()
	{
		return recordsProcessed;
	}

	public void setRecordsProcessed(final int recordsProcessed)
	{
		this.recordsProcessed = recordsProcessed;
	}

	public int getRecordsFailed()
	{
		return recordsFailed;
	}

	public void setRecordsFailed(final int recordsFailed)
	{
		this.recordsFailed = recordsFailed;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(final String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public Map<String, Object> getMetadata()
	{
		return metadata;
	}

	public void setMetadata(final Map<String, Object> metadata)
	{
		this.metadata = metadata;
	}

	public byte[] getRawResponsePayload()
	{
		return rawResponsePayload;
	}

	public void setRawResponsePayload(final byte[] rawResponsePayload)
	{
		this.rawResponsePayload = rawResponsePayload;
	}

	public void markCompleted()
	{
		this.status = "COMPLETED";
		this.endTime = new Date();
	}

	public void markFailed(final String error)
	{
		this.status = "FAILED";
		this.errorMessage = error;
		this.endTime = new Date();
	}

	public boolean isSuccessful()
	{
		return "COMPLETED".equals(status) && recordsFailed == 0;
	}
}
