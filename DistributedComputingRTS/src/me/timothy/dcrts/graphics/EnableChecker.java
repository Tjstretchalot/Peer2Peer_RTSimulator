package me.timothy.dcrts.graphics;

import java.util.Map;

public interface EnableChecker {
	public boolean isEnabled(Object obj, Map<String, Object> meta);
}
