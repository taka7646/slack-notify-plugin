package com.taka7646.slack;

public enum SendTarget {

	FEED("feed"),
	COMMENT("comments"),
	;

	private String value;

	SendTarget(String value) {
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}

	public static SendTarget fromString(String text) {
		if (text != null) {
			for (SendTarget b : SendTarget.values()) {
				if (text.equalsIgnoreCase(b.value)) {
					return b;
				}
			}
		}
		return FEED;
	}
}
