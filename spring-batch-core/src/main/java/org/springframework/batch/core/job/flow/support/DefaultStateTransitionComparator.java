/*
 * Copyright 2013-2024 the original author or authors.
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
 * Sorts by descending specificity of pattern, based on counting wildcards (with ? being
 * considered more specific than *). This means that more specific patterns will be
 * considered greater than less specific patterns. Hence foo &gt; fo? &gt; ??? &gt; foo*
 * &gt; *
 *
 * For more complex comparisons, any string containing at least one * token will be
 * considered more generic than any string that has no * token. If both strings have at
 * least one * token, then the string with fewer * tokens will be considered the most
 * generic. If both strings have the same number of * tokens, then the comparison will
 * fall back to length of the overall string with the shortest value being the most
 * generic. Finally, if the * token count is equal and the string length is equal then the
 * final comparison will be alphabetic.
 *
 * When two strings have ? tokens, then the string with the most ? tokens will be
 * considered the most generic. If both strings have the same number of ? tokens, then the
 * comparison will fall back to length of the overall string with the shortest value being
 * the most generic. Finally, if the ? token count is equal and the string length is equal
 * then the final comparison will be alphabetic
 *
 * If the strings contain neither * nor ? tokens then alphabetic comparison will be used.
 *
 * Hence bar &gt; foo &gt; fo? &gt; bar?? &gt; foo?? &gt; ?0? &gt; ??? &gt; *foo* &gt; *f*
 * &gt; foo* &gt; *
 *
 * @see Comparator
 * @author Michael Minella
 * @author Robert McNees
 * @since 3.0
 */
public class DefaultStateTransitionComparator implements Comparator<StateTransition> {

	public static final String STATE_TRANSITION_COMPARATOR = "batch_state_transition_comparator";

	@Override
	public int compare(StateTransition arg0, StateTransition arg1) {
		String arg0Pattern = arg0.getPattern();
		String arg1Pattern = arg1.getPattern();
		if (arg0.getPattern().equals(arg1Pattern)) {
			return 0;
		}
		int arg0AsteriskCount = StringUtils.countOccurrencesOf(arg0Pattern, "*");
		int arg1AsteriskCount = StringUtils.countOccurrencesOf(arg1Pattern, "*");
		if (arg0AsteriskCount > 0 && arg1AsteriskCount == 0) {
			return -1;
		}
		if (arg0AsteriskCount == 0 && arg1AsteriskCount > 0) {
			return 1;
		}
		if (arg0AsteriskCount > 0 && arg1AsteriskCount > 0) {
			if (arg0AsteriskCount < arg1AsteriskCount) {
				return -1;
			}
			if (arg0AsteriskCount > arg1AsteriskCount) {
				return 1;
			}
		}
		int arg0WildcardCount = StringUtils.countOccurrencesOf(arg0Pattern, "?");
		int arg1WildcardCount = StringUtils.countOccurrencesOf(arg1Pattern, "?");
		if (arg0WildcardCount > arg1WildcardCount) {
			return -1;
		}
		if (arg0WildcardCount < arg1WildcardCount) {
			return 1;
		}
		if (arg0Pattern.length() != arg1Pattern.length() && (arg0AsteriskCount > 0 || arg0WildcardCount > 0)) {
			return Integer.compare(arg0Pattern.length(), arg1Pattern.length());
		}
		return arg1.getPattern().compareTo(arg0Pattern);
	}

}
