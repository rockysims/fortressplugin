package me.newyith.fortresstemp.sandbox.controller;

import me.newyith.fortresstemp.sandbox.model.CarModel;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Car {
	private CarModel model;
	private List<Wheel> wheels;

	public Car(CarModel model) {
		this.model = model;
		this.wheels = model.wheels.stream().map(Wheel::new).collect(Collectors.toList());
	}

	public CarModel getModel() {
		return model;
	}

	public void changeWheel() {
		setWheel(0, new Wheel(0));
	}

	private void setWheel(int index, Wheel wheel) {
		wheels.set(index, wheel);
		model.wheels.set(index, wheel.getModel());
	}

	class WheelList extends AbstractList<Wheel> {
		private List<Wheel> delegate = new ArrayList<>();

		@Override
		public void forEach(Consumer<? super Wheel> action) {
			delegate.forEach(action);
		}

		@Override
		public Stream<Wheel> parallelStream() {
			return delegate.parallelStream();
		}

		@Override
		public Stream<Wheel> stream() {
			return delegate.stream();
		}

		@Override
		public boolean removeIf(Predicate<? super Wheel> filter) {
			return delegate.removeIf(filter);
		}

		@Override
		public Spliterator<Wheel> spliterator() {
			return delegate.spliterator();
		}

		@Override
		public Iterator<Wheel> iterator() {
			return delegate.iterator();
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public boolean add(Wheel wheel) {
			return delegate.add(wheel);
		}

		@Override
		public Wheel get(int index) {
			return delegate.get(index);
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Wheel) {
				//remove from both wheels and model.wheels
			}
			return delegate.remove(o);
		}
	}
}
