package com.appindesign.diblibrary;

public class Punch 
{
	private String code;
	private long timestamp;

	public Punch( String code, long timestamp ) {
		this.code = code;
		this.timestamp = timestamp;
	}

	public String getCode() {
		return code;
	}

	public void setCode( String code ) {
		this.code = code;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp( long time ) {
		this.timestamp = time;
	}
}