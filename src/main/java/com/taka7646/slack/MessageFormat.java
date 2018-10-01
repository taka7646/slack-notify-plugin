package com.taka7646.slack;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import hudson.model.AbstractBuild;
import hudson.model.Result;

public enum MessageFormat {

	TEXT("text"),
	MARKDOWN("markdown"),
	;

	private String value;

	MessageFormat(String value) {
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}

	public static MessageFormat fromString(String text) {
		if (text != null) {
			for (MessageFormat b : MessageFormat.values()) {
				if (text.equalsIgnoreCase(b.value)) {
					return b;
				}
			}
		}
		return TEXT;
	}
	
	public void setFormatParam(List<NameValuePair> params){
		if(this == MARKDOWN){
			params.add(new BasicNameValuePair("formatting", this.toString()));
		}
	}
}
