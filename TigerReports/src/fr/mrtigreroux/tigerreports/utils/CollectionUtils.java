package fr.mrtigreroux.tigerreports.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

public class CollectionUtils {

	private static final String ELEMENTS_SEPARATOR = ", ";

	public static <T> T getElementAtIndex(List<T> elements, int index) {
		if (index >= 0 && index < elements.size()) {
			return elements.get(index);
		} else {
			return null;
		}
	}

	public static <T> String toString(Collection<T> collection) {
		if (collection == null) {
			return "null";
		}

		return toString(collection.iterator(), new BiConsumer<StringBuilder, T>() {

			@Override
			public void accept(StringBuilder sb, T element) {
				sb.append(element);
			}

		});
	}

	public static <K, V> String toString(Map<K, V> map) {
		if (map == null) {
			return "null";
		}

		return toString(map.entrySet().iterator(), new BiConsumer<StringBuilder, Entry<K, V>>() {

			@Override
			public void accept(StringBuilder sb, Entry<K, V> element) {
				sb.append(element.getKey()).append(": ").append(element.getValue());
			}

		});
	}

	public static <E> String toString(Iterator<E> it, BiConsumer<StringBuilder, E> elementAppender) {
		if (it == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder("[");
		int size = 0;
		while (it.hasNext()) {
			elementAppender.accept(sb, it.next());
			sb.append(ELEMENTS_SEPARATOR);
			size++;
		}

		int length = sb.length();
		if (length >= ELEMENTS_SEPARATOR.length()) {
			sb.setLength(length - ELEMENTS_SEPARATOR.length()); // Remove last ", "
		}

		return sb.append("](").append(size).append(")").toString();
	}

}
