package me.newyith.fortress.sandbox.promises;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by rockysims on 11/5/15.
 */
public class ConcurrencyPlay {
	public static void main(String[] args) {

		CompletableFuture<List<Integer>> future = randomInts(5);

		System.out.println("Before Generation");
		future.thenAccept(values -> System.out.println("thenAccept: " + values));
		System.out.println("join: " + future.join());
		System.out.println("After Generation");

		future.getNow(null); //return null if !future.isDone()

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
