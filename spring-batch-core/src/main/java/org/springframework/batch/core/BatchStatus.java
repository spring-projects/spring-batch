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

package org.springframework.batch.core;


/**
 * Enumeration representing the status of a an Execution.
 * 
 * @author Lucas Ward
 */

public enum BatchStatus {

	COMPLETED, STARTED, STARTING, FAILED, STOPPING, STOPPED, UNKNOWN;
	
	public static BatchStatus max(BatchStatus status1, BatchStatus status2) {
		if (status1.compareTo(status2)<0) {
			return status2;
		}
		if (status1.compareTo(status2)>0) {
			return status1;
		}
		else return status1;
	}

}
