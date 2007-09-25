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

package org.springframework.batch.execution.bootstrap.support;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.execution.bootstrap.JobLauncher;
import org.springframework.batch.repeat.ExitStatus;

public class SimpleJvmExitCodeMapperTests extends TestCase {

	private SimpleJvmExitCodeMapper ecm;
	private SimpleJvmExitCodeMapper ecm2;
	
	protected void setUp() throws Exception {
		ecm = new SimpleJvmExitCodeMapper();
		Map ecmMap = new HashMap();
		ecmMap.put("MY_CUSTOM_CODE", new Integer(3));
		ecm.setMapping(ecmMap);
		
		ecm2 = new SimpleJvmExitCodeMapper();
		Map ecm2Map = new HashMap();
		ecm2Map.put(ExitStatus.FINISHED.getExitCode(), new Integer(-1));
		ecm2Map.put(ExitStatus.FAILED.getExitCode(), new Integer(-2));
		ecm2Map.put(JobLauncher.JOB_CONFIGURATION_NOT_PROVIDED, new Integer(-3));
		ecm2Map.put(JobLauncher.NO_SUCH_JOB_CONFIGURATION, new Integer(-3));
		ecm2.setMapping(ecm2Map);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetExitCodeWithpPredefinedCodes() {
		assertEquals(
				ecm.getExitCode(ExitStatus.FINISHED.getExitCode()),
				ExitCodeMapper.JVM_EXITCODE_COMPLETED);
		assertEquals(
				ecm.getExitCode(ExitStatus.FAILED.getExitCode()),
				ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);
		assertEquals(
				ecm.getExitCode(JobLauncher.JOB_CONFIGURATION_NOT_PROVIDED),
				ExitCodeMapper.JVM_EXITCODE_JOB_CONFIGURATION_ERROR);		
		assertEquals(
				ecm.getExitCode(JobLauncher.NO_SUCH_JOB_CONFIGURATION),
				ExitCodeMapper.JVM_EXITCODE_JOB_CONFIGURATION_ERROR);		
	}
	
	public void testGetExitCodeWithPredefinedCodesOverridden() {
		System.out.println(ecm2.getExitCode(ExitStatus.FINISHED.getExitCode()));
		assertEquals(
				ecm2.getExitCode(ExitStatus.FINISHED.getExitCode()), -1);
		assertEquals(
				ecm2.getExitCode(ExitStatus.FAILED.getExitCode()), -2);
		assertEquals(
				ecm2.getExitCode(JobLauncher.JOB_CONFIGURATION_NOT_PROVIDED), -3);		
		assertEquals(
				ecm2.getExitCode(JobLauncher.NO_SUCH_JOB_CONFIGURATION), -3);		
	}

	public void testGetExitCodeWithCustomCode() {
		assertEquals(ecm.getExitCode("MY_CUSTOM_CODE"),3);		
	}

	public void testGetExitCodeWithDefaultCode() {
		assertEquals(
				ecm.getExitCode("UNDEFINED_CUSTOM_CODE"),
				ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);		
	}
	
	
}
