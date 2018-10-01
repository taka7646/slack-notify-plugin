package com.taka7646.slack;

class StringUtils{
    public static boolean isEmpty(String str) {
        if (str == null){
            return true;
        }
        return str.length() == 0;
    }
    
    public static boolean isNotEmpty(String str) {
    	return !isEmpty(str);
    }
}