package fr.mrtigreroux.tigerreports.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * @author MrTigreroux
 */
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
			} else if (firstElement instanceof Object[]) {
				consumer = new BiConsumer<StringBuilder, T>() {

					@Override
					public void accept(StringBuilder sb, T element) {
						sb.append(Arrays.deepToString((Object[]) element));
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
			} else if (firstValue instanceof Object[]) {
				consumer = new BiConsumer<StringBuilder, Entry<K, V>>() {

					@Override
					public void accept(StringBuilder sb, Entry<K, V> element) {
						sb.append(element.getKey())
						        .append(": ")
						        .append(Arrays.deepToString((Object[]) element.getValue()));
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

	public static class ReversedList<T> implements Iterable<T> {

		private final List<T> original;

		public ReversedList(List<T> original) {
			this.original = original;
		}

		@Override
		public Iterator<T> iterator() {
			final ListIterator<T> i = original.listIterator(original.size());

			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return i.hasPrevious();
				}

				@Override
				public T next() {
					return i.previous();
				}

				@Override
				public void remove() {
					i.remove();
				}
			};
		}

	}

	public static <T> ReversedList<T> reversedList(List<T> original) {
		return new ReversedList<T>(original);
	}

	public static class LimitedOrderedList<T> extends LinkedList<T> {

		private static final long serialVersionUID = 4924376041583169739L;

		private final int maxSize;

		public LimitedOrderedList(int maxSize) {
			super();
			this.maxSize = maxSize;
		}

		@Override
		public boolean offerLast(T e) {
			return add(e);
		}

		@Override
		public boolean offer(T e) {
			return add(e);
		}

		@Override
		public void addLast(T e) {
			add(e);
		}

		@Override
		public boolean add(T e) {
			if (this.size() >= maxSize) {
				super.removeFirst();
			}
			return super.add(e);
		}

		@Override
		public void addFirst(T e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void push(T e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(int index, T element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean offerFirst(T e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public T set(int index, T element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends T> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sort(Comparator<? super T> c) {
			throw new UnsupportedOperationException();
		}

	}

}
