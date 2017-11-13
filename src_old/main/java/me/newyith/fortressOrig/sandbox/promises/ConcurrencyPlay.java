package me.newyith.fortressOrig.sandbox.promises;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConcurrencyPlay {
	public static void main(String[] args) {

		CompletableFuture<List<Integer>> future = randomInts(5);

		System.out.println("Before Generation");
		future.thenAccept(values -> System.out.println("thenAccept: " + values));
		System.out.println("join: " + future.join()); //future.join() means wait for the other thread
		System.out.println("After Generation");

		List<Integer> promisedList = future.getNow(null); //return null if !future.isDone()

		Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
	}

	public static CompletableFuture<List<Integer>> randomInts(int count) {
		return CompletableFuture.supplyAsync(() -> {
			List<Integer> values = new ArrayList<>(count);
			for (int i=0; i<count; i++) {
				Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
				values.add(i);
			}
			return ImmutableList.copyOf(values);
		});
	}
}
