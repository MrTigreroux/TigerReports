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

		BiConsumer<StringBuilder, T> consumer = null;
		Iterator<T> iterator = collection.iterator();
		T firstElement = null;
		while (firstElement == null && iterator.hasNext()) {
			firstElement = iterator.next();
		}

		if (firstElement != null) {
			if (firstElement instanceof Map<?, ?>) {
				consumer = new BiConsumer<StringBuilder, T>() {

					@Override
					public void accept(StringBuilder sb, T element) {
						sb.append(CollectionUtils.toString((Map<?, ?>) element));
					}

				};
			} else if (firstElement instanceof Collection<?>) {
				consumer = new BiConsumer<StringBuilder, T>() {

					@Override
					public void accept(StringBuilder sb, T element) {
						sb.append(CollectionUtils.toString((Collection<?>) element));
					}

				};
			}
		}

		if (consumer == null) {
			consumer = new BiConsumer<StringBuilder, T>() {

				@Override
				public void accept(StringBuilder sb, T element) {
					sb.append(element);
				}

			};
		}

		return toString(collection.iterator(), consumer);
	}

	public static <K, V> String toString(Map<K, V> map) {
		if (map == null) {
			return "null";
		}

		BiConsumer<StringBuilder, Entry<K, V>> consumer = null;
		Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
		V firstValue = null;
		while (firstValue == null && iterator.hasNext()) {
			Entry<K, V> entry = iterator.next();
			if (entry != null) {
				firstValue = entry.getValue();
			}
		}

		if (firstValue != null) {
			if (firstValue instanceof Map<?, ?>) {
				consumer = new BiConsumer<StringBuilder, Entry<K, V>>() {

					@Override
					public void accept(StringBuilder sb, Entry<K, V> element) {
						sb.append(element.getKey())
						        .append(": ")
						        .append(CollectionUtils.toString((Map<?, ?>) element.getValue()));
					}

				};
			} else if (firstValue instanceof Collection<?>) {
				consumer = new BiConsumer<StringBuilder, Entry<K, V>>() {

					@Override
					public void accept(StringBuilder sb, Entry<K, V> element) {
						sb.append(element.getKey())
						        .append(": ")
						        .append(CollectionUtils.toString((Collection<?>) element.getValue()));
					}

				};
			}
		}

		if (consumer == null) {
			consumer = new BiConsumer<StringBuilder, Entry<K, V>>() {

				@Override
				public void accept(StringBuilder sb, Entry<K, V> element) {
					sb.append(element.getKey()).append(": ").append(element.getValue());
				}

			};
		}

		return toString(map.entrySet().iterator(), consumer);
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
