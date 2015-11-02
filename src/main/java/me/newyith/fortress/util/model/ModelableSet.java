package me.newyith.fortress.util.model;

import me.newyith.fortress.util.Debug;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ModelableSet<T extends Modelable> extends AbstractSet<T> implements Modelable {
	public static abstract class Model extends BaseModel {}
	public abstract BaseModel getModel();
	private Set<T> delegate = new HashSet<>();

	@Override
	public void forEach(Consumer<? super T> action) {
		delegate.forEach(action);
	}

	@Override
	public Stream<T> parallelStream() {
		return delegate.parallelStream();
	}

	@Override
	public Stream<T> stream() {
		return delegate.stream();
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		return delegate.removeIf(filter);
	}

	@Override
	public Spliterator<T> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean add(T item) {
		return delegate.add(item);
	}

	@Override
	public boolean remove(Object o) {
		try {
			T t = (T)o;
			return delegate.remove(t);
		} catch (Exception e) {
			e.printStackTrace();
			Debug.msg("ModelableSet.remove() failed.");
		}
//		//remove from both wheels and model.wheels
	}
}



/*

class TList extends AbstractList<T> {
		private List<T> delegate = new ArrayList<>();

		@Override
		public void forEach(Consumer<? super T> action) {
			delegate.forEach(action);
		}

		@Override
		public Stream<T> parallelStream() {
			return delegate.parallelStream();
		}

		@Override
		public Stream<T> stream() {
			return delegate.stream();
		}

		@Override
		public boolean removeIf(Predicate<? super T> filter) {
			return delegate.removeIf(filter);
		}

		@Override
		public Spliterator<T> spliterator() {
			return delegate.spliterator();
		}

		@Override
		public Iterator<T> iterator() {
			return delegate.iterator();
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public boolean add(T wheel) {
			return delegate.add(wheel);
		}

		@Override
		public T get(int index) {
			return delegate.get(index);
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof T) {
				//remove from both wheels and model.wheels
			}
			return delegate.remove(o);
		}
	}

 */