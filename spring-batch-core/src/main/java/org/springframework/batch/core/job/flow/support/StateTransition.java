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
package org.springframework.batch.core.job.flow.support;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.support.PatternMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object representing a potential transition from one {@link State} to
 * another. The originating State name and the next {@link State} to execute are
 * linked by a pattern for the {@link ExitStatus#getExitCode() exit code} of an
 * execution of the originating State.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public final class StateTransition implements Comparable<StateTransition> {

	private final State state;

	private final String pattern;

	private final String next;

	/**
	 * Create a new end state {@link StateTransition} specification. This
	 * transition explicitly goes unconditionally to an end state (i.e. no more
	 * executions).
	 * 
	 * @param state the {@link State} used to generate the outcome for this
	 * transition
	 */
	public static StateTransition createEndStateTransition(State state) {
		return createStateTransition(state, null, null);
	}

	/**
	 * Create a new end state {@link StateTransition} specification. This
	 * transition explicitly goes to an end state (i.e. no more processing) if
	 * the outcome matches the pattern.
	 * 
	 * @param state the {@link State} used to generate the outcome for this
	 * transition
	 * @param pattern the pattern to match in the exit status of the
	 * {@link State}
	 */
	public static StateTransition createEndStateTransition(State state, String pattern) {
		return createStateTransition(state, pattern, null);
	}

	/**
	 * Convenience method to switch the origin and destination of a transition,
	 * creating a new instance.
	 * 
	 * @param stateTransition an existing state transition
	 * @param state the new state for the origin
	 * @param next the new name for the destination
	 * 
	 * @return a {@link StateTransition}
	 */
	public static StateTransition switchOriginAndDestination(StateTransition stateTransition, State state, String next) {
		return createStateTransition(state, stateTransition.pattern, next);
	}

	/**
	 * Create a new state {@link StateTransition} specification with a wildcard
	 * pattern that matches all outcomes.
	 * 
	 * @param state the {@link State} used to generate the outcome for this
	 * transition
	 * @param next the name of the next {@link State} to execute
	 */
	public static StateTransition createStateTransition(State state, String next) {
		return createStateTransition(state, null, next);
	}

	/**
	 * Create a new {@link StateTransition} specification from one {@link State}
	 * to another (by name).
	 * 
	 * @param state the {@link State} used to generate the outcome for this
	 * transition
	 * @param pattern the pattern to match in the exit status of the
	 * {@link State}
	 * @param next the name of the next {@link State} to execute
	 */
	public static StateTransition createStateTransition(State state, String pattern, String next) {
		return new StateTransition(state, pattern, next);
	}

	private StateTransition(State state, String pattern, String next) {
		super();
		if (!StringUtils.hasText(pattern)) {
			this.pattern = "*";
		}
		else {
			this.pattern = pattern;
		}

		Assert.notNull(state, "A state is required for a StateTransition");
		if (state.isEndState() && StringUtils.hasText(next)) {
			throw new IllegalStateException("End state cannot have next: " + state);
		}

		this.next = next;
		this.state = state;
	}

	/**
	 * Public getter for the State.
	 * @return the State
	 */
	public State getState() {
		return state;
	}

	/**
	 * Public getter for the next State name.
	 * @return the next
	 */
	public String getNext() {
		return next;
	}

	/**
	 * Check if the provided status matches the pattern, signalling that the
	 * next State should be executed.
	 * 
	 * @param status the status to compare
	 * @return true if the pattern matches this status
	 */
	public boolean matches(String status) {
		return PatternMatcher.match(pattern, status);
	}

	/**
	 * Check for a special next State signalling the end of a job.
	 * 
	 * @return true if this transition goes nowhere (there is no next)
	 */
	public boolean isEnd() {
		return next == null;
	}

	/**
	 * Sorts by decreasing specificity of pattern, based on just counting
	 * wildcards (with * taking precedence over ?). If wildcard counts are equal
	 * then falls back to alphabetic comparison. Hence * &gt; foo* &gt; ??? &gt;
	 * fo? > foo.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(StateTransition other) {
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
		return String.format("StateTransition: [state=%s, pattern=%s, next=%s]",
				state == null ? null : state.getName(), pattern, next);
	}

}
