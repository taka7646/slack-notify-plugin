package com.taka7646.slack;

import hudson.model.AbstractBuild;
import hudson.model.Result;

public enum NotificationStrategy {

	ALL("all"),
	FAILURE("failure"),
	SUCCESS("success"),
	FAILURE_AND_FIXED("failure and fixed"),
	;

	private String value;

	NotificationStrategy(String value) {
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}

	public static NotificationStrategy fromString(String text) {
		if (text != null) {
			for (NotificationStrategy b : NotificationStrategy.values()) {
				if (text.equalsIgnoreCase(b.value)) {
					return b;
				}
			}
		}
		return ALL;
	}
	
	public boolean needNotification(AbstractBuild<?, ?> build){
		Result result = build.getResult();
		if(this == FAILURE_AND_FIXED){
			Result previousResult = build.getPreviousBuild().getResult();
			if(result == Result.SUCCESS){
				return previousResult != Result.SUCCESS;
			}else if(result == Result.ABORTED){
				return false;
			}else{
				return true;
			}
		}
		else if(result == Result.SUCCESS){
			//　成功のとき
			return this == ALL || this == SUCCESS;
		}else{
			// 失敗のとき
			return this == ALL || this == FAILURE;
		}
	}
}
