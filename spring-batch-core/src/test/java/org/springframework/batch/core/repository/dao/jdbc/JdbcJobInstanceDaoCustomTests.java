/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.core.repository.dao.jdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig(locations = "sql-dao-custom-key-generator-test.xml")
public class JdbcJobInstanceDaoCustomTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private JobInstanceDao jobInstanceDao;

	@Test
	public void testCustomJobKeyGeneratorIsWired() {
		Object jobKeyGenerator = applicationContext.getBean("jobKeyGenerator");

		Assertions.assertTrue(jobKeyGenerator != null);
		Assertions.assertEquals(CustomJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

	@Test
	public void testCustomJobKeyGeneratorIsUsed() {
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(jobInstanceDao,
				"jobKeyGenerator");
		Assertions.assertEquals(CustomJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

}
