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

package org.springframework.batch.core.domain;

/**
 * Typesafe enumeration representating the status of an artifact within
 * the batch container.  See Effective Java Programming by Joshua Bloch 
 * for more details on the pattern used.
 * 
 * @author Lucas Ward
 *
 */

public class BatchStatus {
	
	private final String name;
	
	private BatchStatus(String name) {
		this.name = name;
	}

	public String toString(){
		return name;
	}
	
	public static final BatchStatus COMPLETED = new BatchStatus("COMPLETED");

	public static final BatchStatus STARTED = new BatchStatus("STARTED");

	public static final BatchStatus STARTING = new BatchStatus("STARTING");

	public static final BatchStatus FAILED = new BatchStatus("FAILED");
	
	public static final BatchStatus STOPPED = new BatchStatus("STOPPED");
	
	private static final BatchStatus[] VALUES = {STARTING, STARTED, COMPLETED, FAILED, STOPPED};

	public static BatchStatus getStatus(String statusAsString){
		
		for(int i = 0; i < VALUES.length; i++){
			if(VALUES[i].toString().equals(statusAsString)){
				return (BatchStatus)VALUES[i];
			}
		}
		
		return null;
	}
}
