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
package org.springframework.batch.core.job;

import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.StringUtils;

/**
 * Value object representing a potential transition from one {@link Step} to
 * another. The originating step name and the next {@link Step} to execute are
 * linked by a pattern for the {@link ExitStatus#getExitCode() exit code} of an
 * execution of the originating step.
 * 
 * @author Dave Syer
 * 
 */
public class StepTransition implements Comparable<StepTransition> {

	private final String pattern;

	private final String next;

	private final Step step;

	/**
	 * Create a new end state {@link StepTransition} specification. This
	 * transition explicitly goes to an end state (i.e. no more executions).
	 * 
	 * @see StepTransition#StepTransition(Step, String, String)
	 */
	public StepTransition(Step step, String pattern) {
		this(step, pattern, null);
	}

	/**
	 * Create a new {@link StepTransition} specification from one step to
	 * another (by name).
	 * 
	 * @param step the step to be executed
	 * @param pattern the pattern to match in the {@link ExitStatus} of the step
	 * @param next the name of the next step to execute
	 */
	public StepTransition(Step step, String pattern, String next) {
		super();
		this.step = step;
		if (!StringUtils.hasText(pattern)) {
			this.pattern = "*";
		}
		else {
			this.pattern = pattern;
		}
		this.next = next;
	}

	/**
	 * Public getter for the next step name.
	 * @return the next
	 */
	public String getNext() {
		return next;
	}

	/**
	 * Public getter for the step.
	 * @return the step
	 */
	public Step getStep() {
		return step;
	}

	/**
	 * Check if the provided {@link ExitStatus} matches the pattern, signalling
	 * that the next step should be executed.
	 * 
	 * @param status the {@link ExitStatus} to compare
	 * @return true if the pattern matches this status
	 */
	public boolean matches(ExitStatus status) {
		return matchStrings(pattern, status.getExitCode());
	}

	/**
	 * Check for a special next step signalling the end of a job.
	 * 
	 * @return true if this transition goes nowhere (there is no next)
	 */
	public boolean isEnd() {
		return next == null;
	}

	/**
	 * Lifted from AntPathMatcher in Spring Core. Tests whether or not a string
	 * matches against a pattern. The pattern may contain two special
	 * characters:<br>
	 * '*' means zero or more characters<br>
	 * '?' means one and only one character
	 * @param pattern pattern to match against. Must not be <code>null</code>.
	 * @param str string which must be matched against the pattern. Must not be
	 * <code>null</code>.
	 * @return <code>true</code> if the string matches against the pattern, or
	 * <code>false</code> otherwise.
	 */
	private boolean matchStrings(String pattern, String str) {
		char[] patArr = pattern.toCharArray();
		char[] strArr = str.toCharArray();
		int patIdxStart = 0;
		int patIdxEnd = patArr.length - 1;
		int strIdxStart = 0;
		int strIdxEnd = strArr.length - 1;
		char ch;

		boolean containsStar = pattern.contains("*");

		if (!containsStar) {
			// No '*'s, so we make a shortcut
			if (patIdxEnd != strIdxEnd) {
				return false; // Pattern and string do not have the same size
			}
			for (int i = 0; i <= patIdxEnd; i++) {
				ch = patArr[i];
				if (ch != '?') {
					if (ch != strArr[i]) {
						return false;// Character mismatch
					}
				}
			}
			return true; // String matches against pattern
		}

		if (patIdxEnd == 0) {
			return true; // Pattern contains only '*', which matches anything
		}

		// Process characters before first star
		while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
			if (ch != '?') {
				if (ch != strArr[strIdxStart]) {
					return false;// Character mismatch
				}
			}
			patIdxStart++;
			strIdxStart++;
		}
		if (strIdxStart > strIdxEnd) {
			// All characters in the string are used. Check if only '*'s are
			// left in the pattern. If so, we succeeded. Otherwise failure.
			for (int i = patIdxStart; i <= patIdxEnd; i++) {
				if (patArr[i] != '*') {
					return false;
				}
			}
			return true;
		}

		// Process characters after last star
		while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
			if (ch != '?') {
				if (ch != strArr[strIdxEnd]) {
					return false;// Character mismatch
				}
			}
			patIdxEnd--;
			strIdxEnd--;
		}
		if (strIdxStart > strIdxEnd) {
			// All characters in the string are used. Check if only '*'s are
			// left in the pattern. If so, we succeeded. Otherwise failure.
			for (int i = patIdxStart; i <= patIdxEnd; i++) {
				if (patArr[i] != '*') {
					return false;
				}
			}
			return true;
		}

		// process pattern between stars. padIdxStart and patIdxEnd point
		// always to a '*'.
		while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
			int patIdxTmp = -1;
			for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
				if (patArr[i] == '*') {
					patIdxTmp = i;
					break;
				}
			}
			if (patIdxTmp == patIdxStart + 1) {
				// Two stars next to each other, skip the first one.
				patIdxStart++;
				continue;
			}
			// Find the pattern between padIdxStart & padIdxTmp in str between
			// strIdxStart & strIdxEnd
			int patLength = (patIdxTmp - patIdxStart - 1);
			int strLength = (strIdxEnd - strIdxStart + 1);
			int foundIdx = -1;
			strLoop: for (int i = 0; i <= strLength - patLength; i++) {
				for (int j = 0; j < patLength; j++) {
					ch = patArr[patIdxStart + j + 1];
					if (ch != '?') {
						if (ch != strArr[strIdxStart + i + j]) {
							continue strLoop;
						}
					}
				}

				foundIdx = strIdxStart + i;
				break;
			}

			if (foundIdx == -1) {
				return false;
			}

			patIdxStart = patIdxTmp;
			strIdxStart = foundIdx + patLength;
		}

		// All characters in the string are used. Check if only '*'s are left
		// in the pattern. If so, we succeeded. Otherwise failure.
		for (int i = patIdxStart; i <= patIdxEnd; i++) {
			if (patArr[i] != '*') {
				return false;
			}
		}

		return true;
	}

	/**
	 * Sorts by decreasing specificity of pattern, based on just counting
	 * wildcards (with * taking precedence over ?). If wodlcard counts are equal
	 * then falls back to alphabetic comparison. Hence * &gt; foo* &gt; ??? &gt;
	 * fo? > foo.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(StepTransition other) {
		String value = other.pattern;
		if (pattern.equals(value)) {
			return 0;
		}
		int patternCount = StringUtils.countOccurrencesOf(pattern, "*");
		int valueCount = StringUtils.countOccurrencesOf(value, "*");
		if (patternCount > valueCount) {
			return 1;
		}
		if (patternCount < valueCount) {
			return -1;
		}
		patternCount = StringUtils.countOccurrencesOf(pattern, "?");
		valueCount = StringUtils.countOccurrencesOf(value, "?");
		if (patternCount > valueCount) {
			return 1;
		}
		if (patternCount < valueCount) {
			return -1;
		}
		return pattern.compareTo(value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("StepTransition: step=%s, pattern=%s, next=%s", step.getName(), pattern, next);
	}

}
