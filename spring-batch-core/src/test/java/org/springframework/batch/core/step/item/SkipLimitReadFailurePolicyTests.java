/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.step.item.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.item.SkipLimitExceededException;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * @author Lucas Ward
 *
 */
public class SkipLimitReadFailurePolicyTests extends TestCase {

	private LimitCheckingItemSkipPolicy failurePolicy;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		List skippableExceptions = new ArrayList();
		skippableExceptions.add(FlatFileParseException.class);
		
		failurePolicy = new LimitCheckingItemSkipPolicy(1, skippableExceptions);
	}
	
	public void testLimitExceed(){		
		try{
			failurePolicy.shouldSkip(new FlatFileParseException("", ""), 2);
			fail();
		}
		catch(SkipLimitExceededException ex){
			//expected
		}
	}
	
	public void testNonSkippableException(){
		assertFalse(failurePolicy.shouldSkip(new FileNotFoundException(), 2));
	}
	
	public void testSkip(){
		assertTrue(failurePolicy.shouldSkip(new FlatFileParseException("",""), 0));
	}

}
