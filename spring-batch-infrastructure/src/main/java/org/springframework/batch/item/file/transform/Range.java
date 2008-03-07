package org.springframework.batch.item.file.transform;

import org.springframework.util.Assert;

/**
 * A class to represent ranges. A Range can have minimum/maximum values from
 * interval &lt;1,Integer.MAX_VALUE-1&gt; A Range can be unbounded at maximum
 * side. This can be specified by passing {@link Range#UPPER_BORDER_NOT_DEFINED}} as max
 * value or using constructor {@link #Range(int)}.
 * 
 * @author peter.zozom
 */
public class Range {

	private final static int UPPER_BORDER_NOT_DEFINED = Integer.MAX_VALUE;
	
	private int min;	
	private int max;
	
	public Range(int min) {
		checkMinMaxValues(min, UPPER_BORDER_NOT_DEFINED);
		this.min = min;
		this.max = UPPER_BORDER_NOT_DEFINED;		
	}
	
	public Range(int min, int max) {
		checkMinMaxValues(min, max);
		this.min = min;
		this.max = max;
	}

	public int getMax() {		
		return max;
	}

	public int getMin() {
		return min;
	}

	public void setMax(int max) {
		checkMinMaxValues(this.min, max);
		this.max = max;
	}

	public void setMin(int min) {
		checkMinMaxValues(min, this.max);
		this.min = min;
	}
	
	public boolean hasMaxValue() {
		return max != UPPER_BORDER_NOT_DEFINED;
	}
	
	public String toString() {
		return hasMaxValue() ? min + "-" + max : String.valueOf(min);
	}
	
	private void checkMinMaxValues(int min, int max) {
		Assert.isTrue(min>0, "Min value must be higher than zero");
		Assert.isTrue(min<=max, "Min value should be lower or equal to max value");		
	}
}
