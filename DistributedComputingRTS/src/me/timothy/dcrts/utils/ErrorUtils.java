package me.timothy.dcrts.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience methods for throwing errors with good messages
 * @author Timothy
 */
public class ErrorUtils {

	/**
	 * Throw a NullPointerException that specifies which parameters are null.
	 * @param params the names of the parameters
	 * @param objs the objects, index-related with params.
	 */
	public static void nullPointer(String[] params, Object... objs) {
		StringBuilder msg = new StringBuilder();
		List<Integer> nullIndexes = new ArrayList<>();
		for(int i = 0; i < params.length; i++) {
			Object o = objs[i];
			if(o == null) {
				nullIndexes.add(i);
			}
		}
		if(nullIndexes.size() <= 0) {
		}else if(nullIndexes.size() == 1) {
			msg.append(params[nullIndexes.get(0)]).append(" is null.");
		}else if(nullIndexes.size() == 2) {
			msg.append(params[nullIndexes.get(0)]).append(" and ");
			msg.append(params[nullIndexes.get(1)]).append(" are null");
		}else if(nullIndexes.size() > 2) {
			msg.append(params[nullIndexes.get(0)]);
			for(int i = 1; i < nullIndexes.size() - 1; i++) {
				msg.append(", ");
				msg.append(params[nullIndexes.get(i)]);
			}
			msg.append(" and ");
			msg.append(params[nullIndexes.get(nullIndexes.size() - 1)]);
			msg.append(" are null.");
		}
		throw new NullPointerException(msg.toString());
	}

}
