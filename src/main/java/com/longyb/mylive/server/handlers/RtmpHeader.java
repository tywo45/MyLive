package com.longyb.mylive.server.handlers;

import lombok.Data;

@Data
public class RtmpHeader {
	int csid;
	int fmt;
	int timestamp;

	int messageLength;
	short messageTypeId;
	int messageStreamId;

	int timestampDelta;

	long extendedTimestamp;
	
	//used when response an ack
	int headerLength;
	
	public boolean mayHaveExtendedTimestamp() {
		return  (fmt==0 && timestamp ==  0xFFFFFF) || ( (fmt==1 || fmt==2) && timestampDelta ==  0xFFFFFF); 
	}

	public int getCsid() {
		return csid;
	}

	public void setCsid(int csid) {
		this.csid = csid;
	}

	public int getFmt() {
		return fmt;
	}

	public void setFmt(int fmt) {
		this.fmt = fmt;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public int getMessageLength() {
		return messageLength;
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}

	public short getMessageTypeId() {
		return messageTypeId;
	}

	public void setMessageTypeId(short messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

	public int getMessageStreamId() {
		return messageStreamId;
	}

	public void setMessageStreamId(int messageStreamId) {
		this.messageStreamId = messageStreamId;
	}

	public int getTimestampDelta() {
		return timestampDelta;
	}

	public void setTimestampDelta(int timestampDelta) {
		this.timestampDelta = timestampDelta;
	}

	public long getExtendedTimestamp() {
		return extendedTimestamp;
	}

	public void setExtendedTimestamp(long extendedTimestamp) {
		this.extendedTimestamp = extendedTimestamp;
	}

	public int getHeaderLength() {
		return headerLength;
	}

	public void setHeaderLength(int headerLength) {
		this.headerLength = headerLength;
	}
}
