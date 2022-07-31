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

package org.springframework.batch.core.launch.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleJvmExitCodeMapperTests {

	private SimpleJvmExitCodeMapper ecm;

	private SimpleJvmExitCodeMapper ecm2;

	@BeforeEach
	void setUp() {
		ecm = new SimpleJvmExitCodeMapper();
		Map<String, Integer> ecmMap = new HashMap<>();
		ecmMap.put("MY_CUSTOM_CODE", 3);
		ecm.setMapping(ecmMap);

		ecm2 = new SimpleJvmExitCodeMapper();
		Map<String, Integer> ecm2Map = new HashMap<>();
		ecm2Map.put(ExitStatus.COMPLETED.getExitCode(), -1);
		ecm2Map.put(ExitStatus.FAILED.getExitCode(), -2);
		ecm2Map.put(ExitCodeMapper.JOB_NOT_PROVIDED, -3);
		ecm2Map.put(ExitCodeMapper.NO_SUCH_JOB, -3);
		ecm2.setMapping(ecm2Map);
	}

	@Test
	void testGetExitCodeWithPredefinedCodes() {
		assertEquals(ecm.intValue(ExitStatus.COMPLETED.getExitCode()), ExitCodeMapper.JVM_EXITCODE_COMPLETED);
		assertEquals(ecm.intValue(ExitStatus.FAILED.getExitCode()), ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);
		assertEquals(ecm.intValue(ExitCodeMapper.JOB_NOT_PROVIDED), ExitCodeMapper.JVM_EXITCODE_JOB_ERROR);
		assertEquals(ecm.intValue(ExitCodeMapper.NO_SUCH_JOB), ExitCodeMapper.JVM_EXITCODE_JOB_ERROR);
	}

	@Test
	void testGetExitCodeWithPredefinedCodesOverridden() {
		System.out.println(ecm2.intValue(ExitStatus.COMPLETED.getExitCode()));
		assertEquals(ecm2.intValue(ExitStatus.COMPLETED.getExitCode()), -1);
		assertEquals(ecm2.intValue(ExitStatus.FAILED.getExitCode()), -2);
		assertEquals(ecm2.intValue(ExitCodeMapper.JOB_NOT_PROVIDED), -3);
		assertEquals(ecm2.intValue(ExitCodeMapper.NO_SUCH_JOB), -3);
	}

	@Test
	void testGetExitCodeWithCustomCode() {
		assertEquals(ecm.intValue("MY_CUSTOM_CODE"), 3);
	}

	@Test
	void testGetExitCodeWithDefaultCode() {
		assertEquals(ecm.intValue("UNDEFINED_CUSTOM_CODE"), ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR);
	}

}
