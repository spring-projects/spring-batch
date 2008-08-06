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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
public class LimitCheckingItemSkipPolicyTests {

	private LimitCheckingItemSkipPolicy failurePolicy;
	
	@Before
	public void setUp() throws Exception {
		List<Class<?>> skippableExceptions = new ArrayList<Class<?>>();
		skippableExceptions.add(FlatFileParseException.class);
		List<Class<?>> fatalExceptions = new ArrayList<Class<?>>();
		
		failurePolicy = new LimitCheckingItemSkipPolicy(1, skippableExceptions, fatalExceptions);
	}
	
	@Test
	public void testLimitExceed(){		
		try{
			failurePolicy.shouldSkip(new FlatFileParseException("", ""), 2);
			fail();
		}
		catch(SkipLimitExceededException ex){
			//expected
		}
	}
	
	@Test
	public void testNonSkippableException(){
		assertFalse(failurePolicy.shouldSkip(new FileNotFoundException(), 2));
	}
	
	@Test
	public void testSkip(){
		assertTrue(failurePolicy.shouldSkip(new FlatFileParseException("",""), 0));
	}

}
