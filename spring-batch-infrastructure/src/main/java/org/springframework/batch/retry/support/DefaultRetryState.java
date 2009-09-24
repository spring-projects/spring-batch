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
package org.springframework.batch.retry.support;

import org.springframework.batch.classify.Classifier;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryState;

/**
 * 
 * @author Dave Syer
 * 
 */
public class DefaultRetryState implements RetryState {

	final private Object key;

	final private boolean forceRefresh;

	final private Classifier<? super Throwable, Boolean> rollbackClassifier;

	/**
	 * Create a {@link DefaultRetryState} representing the state for a new retry
	 * attempt.
	 * 
	 * @see RetryOperations#execute(RetryCallback, RetryState)
	 * @see RetryOperations#execute(RetryCallback, RecoveryCallback, RetryState)
	 * 
	 * @param key the key for the state to allow this retry attempt to be
	 * recognised
	 * @param forceRefresh true if the attempt is known to be a brand new state
	 * (could not have previously failed)
	 * @param rollbackClassifier the rollback classifier to set. The rollback
	 * classifier answers true if the exception provided should cause a
	 * rollback.
	 */
	public DefaultRetryState(Object key, boolean forceRefresh, Classifier<? super Throwable, Boolean> rollbackClassifier) {
		this.key = key;
		this.forceRefresh = forceRefresh;
		this.rollbackClassifier = rollbackClassifier;
	}

	/**
	 * Defaults the force refresh flag to false.
	 * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
	 */
	public DefaultRetryState(Object key, Classifier<? super Throwable, Boolean> rollbackClassifier) {
		this(key, false, rollbackClassifier);
	}

	/**
	 * Defaults the rollback classifier to null.
	 * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
	 */
	public DefaultRetryState(Object key, boolean forceRefresh) {
		this(key, forceRefresh, null);
	}

	/**
	 * Defaults the force refresh flag (to false) and the rollback classifier
	 * (to null).
	 * 
	 * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
	 */
	public DefaultRetryState(Object key) {
		this(key, false, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.retry.IRetryState#getKey()
	 */
	public Object getKey() {
		return key;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.retry.IRetryState#isForceRefresh()
	 */
	public boolean isForceRefresh() {
		return forceRefresh;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.retry.RetryState#rollbackFor(java.lang.Throwable
	 * )
	 */
	public boolean rollbackFor(Throwable exception) {
		if (rollbackClassifier == null) {
			return true;
		}
		return rollbackClassifier.classify(exception);
	}

	@Override
	public String toString() {
		return String.format("[%s: key=%s, forceRefresh=%b]", getClass().getSimpleName(), key, forceRefresh);
	}
}
