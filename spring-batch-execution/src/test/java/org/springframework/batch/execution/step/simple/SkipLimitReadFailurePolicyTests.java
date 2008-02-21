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
package org.springframework.batch.execution.step.simple;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.io.exception.FlatFileParsingException;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class SkipLimitReadFailurePolicyTests extends TestCase {

	LimitCheckingItemSkipPolicy failurePolicy;
	StepExecution stepExecution;
	StepContribution stepContribution;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		List skippableExceptions = new ArrayList();
		skippableExceptions.add(FlatFileParsingException.class);
		
		failurePolicy = new LimitCheckingItemSkipPolicy(1, skippableExceptions);
		stepExecution = new StepExecution(null, null);
		stepExecution.setSkipCount(2);
		stepContribution = stepExecution.createStepContribution();
	}
	
	public void testLimitExceed(){		
		try{
			failurePolicy.shouldSkip(new FlatFileParsingException("", ""), stepContribution);
			fail();
		}
		catch(SkipLimitExceededException ex){
			//expected
		}
	}
	
	public void testNonSkippableException(){
		assertFalse(failurePolicy.shouldSkip(new FileNotFoundException(), stepContribution));
	}
	
	public void testSkip(){
		stepExecution.setSkipCount(0);
		assertTrue(failurePolicy.shouldSkip(new FlatFileParsingException("",""), stepContribution));
	}

}
