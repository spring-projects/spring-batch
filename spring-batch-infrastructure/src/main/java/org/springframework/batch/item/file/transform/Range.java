/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	public final static int UPPER_BORDER_NOT_DEFINED = Integer.MAX_VALUE;
	
	final private int min;	
	final private int max;
	
	public Range(int min) {
		this(min,UPPER_BORDER_NOT_DEFINED);		
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
