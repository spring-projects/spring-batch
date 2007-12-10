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

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public class RepeatOperationsApplicationEvent extends ApplicationEvent {

	public static final int AFTER = 3;

	public static final int BEFORE = 2;

	public static final int CLOSE = 4;

	public static final int OPEN = 1;

	public static final int ERROR = 5;

	final private int type;
	
	final private String message;
	
	/**
	 * Constructor for {@link RepeatOperationsApplicationEvent}.
	 * 
	 * @param source the source of the event. Normally the current
	 * {@link RepeatContext} if there is one.
	 */
	public RepeatOperationsApplicationEvent(Object source, String message, int type) {
		super(source);
		this.message = message;
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public int getType() {
		return type;
	}
	
	/* (non-Javadoc)
	 * @see java.util.EventObject#toString()
	 */
	public String toString() {
		return ClassUtils.getShortName(getClass())+": type="+type+"; message="+message;
	}

}
