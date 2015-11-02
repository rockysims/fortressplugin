package me.newyith.fortress.temp;

import me.newyith.fortress.util.Debug;

public class SubTempExample {
	public int val = 0;

	public void setVal(int val) {
		this.val = val;
	}

	public void print() {
		Debug.msg("SubTempExample.print() called. val: " + val);
	}
}
