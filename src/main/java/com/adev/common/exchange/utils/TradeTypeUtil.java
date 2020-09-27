package com.adev.common.exchange.utils;


/**
 * created by admin
 * date: 2018/9/4 18:10
 */
public class TradeTypeUtil {
	
    public static String getOrderType(String type){
    	if(type == null){
    		return null;
    	}
    	type = type.toLowerCase();
    	switch(type){
    		
         	case "buy":
         		return "BID";
         	case "sell":
         		return "ASK";
         	case "bid":
				return "BID";
         	case "ask":
				return "ASK";
         	case "b":
				return "BID";
         	case "a":
				return "ASK";
         	case "s":
				return "ASK";
			case "1":
				return "ASK";
			case "0":
				return "BID";
             default:
                 return null;
         }
    }
}