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

import org.springframework.batch.execution.bootstrap.JvmExitCodeMapper;
import org.springframework.batch.repeat.ExitCodeMapper;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import junit.framework.TestCase;

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
		ecm2Map.put(JvmExitCodeMapper.BATCH_EXITCODE_COMPLETED, new Integer(-1));
		ecm2Map.put(JvmExitCodeMapper.BATCH_EXITCODE_GENERIC_ERROR, new Integer(-2));
		ecm2Map.put(JvmExitCodeMapper.BATCH_EXITCODE_NO_SUCH_JOBCONFIGURATION, new Integer(-3));
		ecm2.setMapping(ecm2Map);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetExitCodeWithpPredefinedCodes() {
		assertEquals(
				ecm.getExitCode(ExitCodeMapper.BATCH_EXITCODE_COMPLETED),
				JvmExitCodeMapper.JVM_EXITCODE_COMPLETED);
		assertEquals(
				ecm.getExitCode(ExitCodeMapper.BATCH_EXITCODE_GENERIC_ERROR),
				JvmExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);
		assertEquals(
				ecm.getExitCode(ExitCodeMapper.BATCH_EXITCODE_NO_SUCH_JOBCONFIGURATION),
				JvmExitCodeMapper.JVM_EXITCODE_NO_SUCH_JOBCONFIGURATION);		
	}
	
	public void testGetExitCodeWithPredefinedCodesOverridden() {
		System.out.println(ecm2.getExitCode(ExitCodeMapper.BATCH_EXITCODE_COMPLETED));
		assertEquals(
				ecm2.getExitCode(ExitCodeMapper.BATCH_EXITCODE_COMPLETED), -1);
		assertEquals(
				ecm2.getExitCode(ExitCodeMapper.BATCH_EXITCODE_GENERIC_ERROR), -2);
		assertEquals(
				ecm2.getExitCode(ExitCodeMapper.BATCH_EXITCODE_NO_SUCH_JOBCONFIGURATION), -3);		
	}

	public void testGetExitCodeWithCustomCode() {
		assertEquals(ecm.getExitCode("MY_CUSTOM_CODE"),3);		
	}

	public void testGetExitCodeWithDefaultCode() {
		assertEquals(
				ecm.getExitCode("UNDEFINED_CUSTOM_CODE"),
				JvmExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);		
	}
	
	
}
