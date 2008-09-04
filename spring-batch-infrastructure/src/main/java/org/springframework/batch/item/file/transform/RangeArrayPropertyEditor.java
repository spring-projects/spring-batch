package org.springframework.batch.item.file.transform;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.Comparator;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Property editor implementation which parses string and creates array of
 * ranges. Ranges can be provided in any order. </br> Input string should be
 * provided in following format: 'range1, range2, range3,...' where range is
 * specified as:
 * <ul>
 * <li>'X-Y', where X is minimum value and Y is maximum value (condition X<=Y
 * is verified)</li>
 * <li>or 'Z', where Z is minimum and maximum is calculated as (minimum of
 * adjacent range - 1). Maximum of the last range is never calculated. Range
 * stays unbound at maximum side if maximum value is not provided.</li>
 * </ul>
 * Minimum and maximum values can be from interval &lt;1, Integer.MAX_VALUE-1&gt;
 * <p>
 * Examples:</br> 
 * '1, 15, 25, 38, 55-60' is equal to '1-14, 15-24, 25-37, 38-54, 55-60' </br> 
 * '36, 14, 1-10, 15, 49-57' is equal to '36-48, 14-14, 1-10, 15-35, 49-57'
 * <p>
 * Property editor also allows to validate whether ranges are disjoint. Validation
 * can be turned on/off by using {@link #setForceDisjointRanges(boolean)}. By default 
 * validation is turned off.
 * 
 * @author peter.zozom
 */
public class RangeArrayPropertyEditor extends PropertyEditorSupport {
	
	private boolean forceDisjointRanges = false;
	
	/**
	 * Set force disjoint ranges. If set to TRUE, ranges are validated to be disjoint.
	 * For example: defining ranges '1-10, 5-15' will cause IllegalArgumentException in
	 * case of forceDisjointRanges=TRUE.  
	 * @param forceDisjointRanges 
	 */
	public void setForceDisjointRanges(boolean forceDisjointRanges) {
		this.forceDisjointRanges = forceDisjointRanges;
	}

	public void setAsText(String text) throws IllegalArgumentException {
		
		//split text into ranges
		String[] strRanges = text.split(",");
		Range[] ranges = new Range[strRanges.length];
		
		//parse ranges and create array of Range objects 
		for (int i = 0; i < strRanges.length; i++) {			
			String[] range = strRanges[i].split("-");
		
			int min;
			int max;
			
			if ((range.length == 1) && (StringUtils.hasText(range[0]))) {
				min = Integer.parseInt(range[0].trim());
				// correct max value will be assigned later
				ranges[i] = new Range(min);
			} else if ((range.length == 2) && (StringUtils.hasText(range[0]))
					&& (StringUtils.hasText(range[1]))) {
				min = Integer.parseInt(range[0].trim());
				max = Integer.parseInt(range[1].trim());
				ranges[i] = new Range(min,max);
			} else {
				throw new IllegalArgumentException("Range[" + i + "]: range (" + strRanges[i] + ") is invalid");
			}			
			
		}
	
		setMaxValues(ranges);
		setValue(ranges);
	}
	
	public String getAsText() {
		Range[] ranges = (Range[])getValue();
		
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < ranges.length; i++) {
			if(i>0) {
				sb.append(", ");
			}
			sb.append(ranges[i]);
		}
		return sb.toString();
	}
	
	private void setMaxValues(Range[] ranges) {
		
		//clone array, original array should stay same
		Range[] c = (Range[])ranges.clone();
		
		//sort array of Ranges
		Arrays.sort(c, new Comparator() {
				public int compare(Object o1, Object o2) {
					Range c1 = (Range)o1;
					Range c2 = (Range)o2;
					return c1.getMin()-c2.getMin();
				}								
			}
		);

		//set max values for all unbound ranges (except last range)
		for (int i = 0; i < c.length - 1; i++) {
			if (!c[i].hasMaxValue()) {
				//set max value to (min value - 1) of the next range
				c[i] = new Range(c[i].getMin(),c[i+1].getMin() - 1);
			}
		}
		
		if (forceDisjointRanges) {
			verifyRanges(c);
		}
	}
	
	
	private void verifyRanges(Range[] ranges) {
		//verify that ranges are disjoint		
		for(int i = 1; i < ranges.length;i++) {
			Assert.isTrue(ranges[i-1].getMax() < ranges[i].getMin(),
					"Ranges must be disjoint. Range[" + (i-1) + "]: (" + ranges[i-1] + 
					") Range[" + i +"]: (" + ranges[i] + ")");
		}
	}
}
