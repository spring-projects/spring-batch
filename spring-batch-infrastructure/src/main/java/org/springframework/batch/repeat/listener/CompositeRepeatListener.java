/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.repeat.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;

/**
 * Allows a user to register one or more RepeatListeners to be notified on batch events.
 *
 * @author Dave Syer
 *
 */
public class CompositeRepeatListener implements RepeatListener {

	private List<RepeatListener> listeners = new ArrayList<>();

	/**
	 * Default constrcutor
	 */
	public CompositeRepeatListener() {

	}

	/**
	 * Convenience constructor for setting the {@link RepeatListener}s.
	 * @param listeners {@link List} of RepeatListeners to be used by the
	 * CompositeRepeatListener.
	 */
	public CompositeRepeatListener(List<RepeatListener> listeners) {
		setListeners(listeners);
	}

	/**
	 * Convenience constructor for setting the {@link RepeatListener}s.
	 * @param listeners array of RepeatListeners to be used by the
	 * CompositeRepeatListener.
	 */
	public CompositeRepeatListener(RepeatListener... listeners) {
		setListeners(listeners);
	}

	/**
	 * Public setter for the listeners.
	 * @param listeners {@link List} of RepeatListeners to be used by the
	 * CompositeRepeatListener.
	 */
	public void setListeners(List<RepeatListener> listeners) {
		this.listeners = listeners;
	}

	/**
	 * Public setter for the listeners.
	 * @param listeners array of RepeatListeners to be used by the
	 * CompositeRepeatListener.
	 */
	public void setListeners(RepeatListener[] listeners) {
		this.listeners = Arrays.asList(listeners);
	}

	/**
	 * Register additional listener.
	 * @param listener the RepeatListener to be added to the list of listeners to be
	 * notified.
	 */
	public void register(RepeatListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.repeat.RepeatListener#after(org.springframework.batch.
	 * repeat.RepeatContext, org.springframework.batch.repeat.ExitStatus)
	 */
	@Override
	public void after(RepeatContext context, RepeatStatus result) {
		for (RepeatListener listener : listeners) {
			listener.after(context, result);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.repeat.RepeatListener#before(org.springframework.batch.
	 * repeat.RepeatContext)
	 */
	@Override
	public void before(RepeatContext context) {
		for (RepeatListener listener : listeners) {
			listener.before(context);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.repeat.RepeatListener#close(org.springframework.batch.
	 * repeat.RepeatContext)
	 */
	@Override
	public void close(RepeatContext context) {
		for (RepeatListener listener : listeners) {
			listener.close(context);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.repeat.RepeatListener#onError(org.springframework.batch.
	 * repeat.RepeatContext, java.lang.Throwable)
	 */
	@Override
	public void onError(RepeatContext context, Throwable e) {
		for (RepeatListener listener : listeners) {
			listener.onError(context, e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.repeat.RepeatListener#open(org.springframework.batch.
	 * repeat.RepeatContext)
	 */
	@Override
	public void open(RepeatContext context) {
		for (RepeatListener listener : listeners) {
			listener.open(context);
		}
	}

}
