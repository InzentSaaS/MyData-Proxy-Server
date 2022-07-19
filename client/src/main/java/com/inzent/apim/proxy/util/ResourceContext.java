package com.inzent.apim.proxy.util;

import java.util.function.Consumer;

/**
 * @author sklee
 *
 */
public class ResourceContext {
	/**
	 * @return
	 */
	public static StringBuilder getAppendable() {
		return new StringBuilder();
	}

	public static void recycleAppendable(StringBuilder builder){}

	public static String withAppendable(Consumer<StringBuilder> consumer){
		StringBuilder builder = null;

		try{
			builder = getAppendable();

			consumer.accept(builder);

			return builder.toString();
		}
		finally{
			recycleAppendable(builder);
		}
	}
}
