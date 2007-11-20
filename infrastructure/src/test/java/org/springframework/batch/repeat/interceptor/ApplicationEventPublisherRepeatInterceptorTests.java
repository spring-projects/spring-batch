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
package org.springframework.batch.repeat.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class ApplicationEventPublisherRepeatInterceptorTests extends TestCase {

	private ApplicationEventPublisherRepeatInterceptor interceptor = new ApplicationEventPublisherRepeatInterceptor();
	
	private List list = new ArrayList();

	private RepeatContext context = new RepeatContextSupport(null);
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		interceptor.setApplicationEventPublisher(new ApplicationEventPublisher() {
			public void publishEvent(ApplicationEvent event) {
				list.add(event);
			}
		});
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.interceptor.ApplicationEventPublisherRepeatInterceptor#after(org.springframework.batch.repeat.RepeatContext, ExitStatus)}.
	 */
	public void testAfter() {
		interceptor.after(context, ExitStatus.CONTINUABLE);
		assertEquals(1, list.size());
		RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) list.get(0);
		assertEquals(RepeatOperationsApplicationEvent.AFTER, event.getType());
		assertTrue(event.getMessage().toLowerCase().indexOf("after")>=0);
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.interceptor.ApplicationEventPublisherRepeatInterceptor#before(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testBefore() {
		interceptor.before(context);
		assertEquals(1, list.size());
		RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) list.get(0);
		assertEquals(RepeatOperationsApplicationEvent.BEFORE, event.getType());
		assertTrue(event.getMessage().toLowerCase().indexOf("before")>=0);
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.interceptor.ApplicationEventPublisherRepeatInterceptor#close(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testClose() {
		interceptor.close(context);
		assertEquals(1, list.size());
		RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) list.get(0);
		assertEquals(RepeatOperationsApplicationEvent.CLOSE, event.getType());
		assertTrue(event.getMessage().toLowerCase().indexOf("close")>=0);
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.interceptor.ApplicationEventPublisherRepeatInterceptor#onError(org.springframework.batch.repeat.RepeatContext, java.lang.Throwable)}.
	 */
	public void testOnError() {
		interceptor.onError(context, new RuntimeException("foo"));
		assertEquals(1, list.size());
		RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) list.get(0);
		assertEquals(RepeatOperationsApplicationEvent.ERROR, event.getType());
		assertTrue(event.getMessage().toLowerCase().indexOf("foo")>=0);
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.interceptor.ApplicationEventPublisherRepeatInterceptor#open(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testOpen() {
		interceptor.open(context);
		assertEquals(1, list.size());
		RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) list.get(0);
		assertEquals(RepeatOperationsApplicationEvent.OPEN, event.getType());
		assertTrue(event.getMessage().toLowerCase().indexOf("open")>=0);
	}

}
