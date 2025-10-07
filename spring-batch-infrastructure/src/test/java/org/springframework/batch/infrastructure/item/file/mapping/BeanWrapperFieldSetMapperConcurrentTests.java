/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.file.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;

class BeanWrapperFieldSetMapperConcurrentTests {

	@Test
	void testConcurrentUsage() throws Exception {
		final BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setStrict(true);
		mapper.setTargetType(GreenBean.class);
		// mapper.setDistanceLimit(0);
		final DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "blue", "green" };
		lineTokenizer.setNames(names);

		ExecutorService executorService = Executors.newFixedThreadPool(5);
		Collection<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Future<Boolean> result = executorService.submit(() -> {
				for (int i1 = 0; i1 < 10; i1++) {
					GreenBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("blue,green"));
					assertEquals("green", bean.getGreen());
				}
				return true;
			});
			results.add(result);
		}
		for (Future<Boolean> future : results) {
			assertTrue(future.get());
		}
	}

	public static class GreenBean {

		private String green;

		private String blue;

		public String getBlue() {
			return blue;
		}

		public void setBlue(String blue) {
			this.blue = blue;
		}

		public String getGreen() {
			return green;
		}

		public void setGreen(String green) {
			this.green = green;
		}

	}

}
