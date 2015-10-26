package me.newyith.fortress.util.model;

import me.newyith.fortress.util.Point;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ModelableSet<T extends Modelable> extends AbstractSet<T> implements Modelable {
	public static class Model extends BaseModel {
		Point.Model anchor = null;

		public Model(Point anchor) {
			this.anchor = anchor.getModel();
		}
	}
	private Model model;
	private Point anchor;

	public ModelableSet(Model model) {
		this.model = model;
		this.anchor = new Point(model.anchor);
	}

	public Model getModel() {
		return this.model;
	}

	public static void fromModel(Model m) {

	}

	//-----------------------------------------------------------------------





	private Set<T> delegate = new HashSet<>();

	public getModel() {

	}

	@Override
	public void forEach(Consumer<? super T> action) {
		delegate.forEach(action);
		this.getModel();
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
//		if (o instanceof T) {
//			//remove from both wheels and model.wheels
//		}
		return delegate.remove(o);
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