/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.job.flow.support;

import org.springframework.util.StringUtils;

import java.util.Comparator;

/**
 * Sorts by decreasing specificity of pattern, based on just counting
 * wildcards (with * taking precedence over ?). If wildcard counts are equal
 * then falls back to alphabetic comparison. Hence * &gt; foo* &gt; ??? &gt;
 * fo? &gt; foo.
 *
 * @see Comparator
 * @author Michael Minella
 * @since 3.0
 */
public class DefaultStateTransitionComparator implements Comparator<StateTransition> {
	public static final String STATE_TRANSITION_COMPARATOR = "batch_state_transition_comparator";

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(StateTransition arg0, StateTransition arg1) {
		String value = arg1.getPattern();
		if (arg0.getPattern().equals(value)) {
			return 0;
		}
		int patternCount = StringUtils.countOccurrencesOf(arg0.getPattern(), "*");
		int valueCount = StringUtils.countOccurrencesOf(value, "*");
		if (patternCount > valueCount) {
			return 1;
		}
		if (patternCount < valueCount) {
			return -1;
		}
		patternCount = StringUtils.countOccurrencesOf(arg0.getPattern(), "?");
		valueCount = StringUtils.countOccurrencesOf(value, "?");
		if (patternCount > valueCount) {
			return 1;
		}
		if (patternCount < valueCount) {
			return -1;
		}
		return arg0.getPattern().compareTo(value);
	}
}
