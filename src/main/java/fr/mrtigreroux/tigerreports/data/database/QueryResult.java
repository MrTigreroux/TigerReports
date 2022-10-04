package fr.mrtigreroux.tigerreports.data.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author MrTigreroux
 */

public final class QueryResult {

	private final List<Map<String, Object>> resultList;

	public QueryResult(List<Map<String, Object>> resultList) {
		this.resultList = resultList != null ? resultList : new ArrayList<>();
	}

	public List<Map<String, Object>> getResultList() {
		return resultList;
	}

	public Map<String, Object> getResult(int row) {
		try {
			return resultList.get(row);
		} catch (Exception none) {
			return null;
		}
	}

	public Object getResult(int row, String key) {
		try {
			return getResult(row).get(key);
		} catch (Exception none) {
			return null;
		}
	}

	public static boolean isTrue(Object resultValue) {
		if (resultValue != null) {
			if (resultValue instanceof Boolean) {
				return ((Boolean) resultValue).booleanValue();
			} else {
				return "1".equals(resultValue.toString());
			}
		} else {
			return false;
		}
	}

}
