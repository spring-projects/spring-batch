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

package org.springframework.batch.item.file.mapping;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

public class BeanWrapperFieldSetMapperConcurrentTests {

	@Test
	public void testConcurrentUsage() throws Exception {
		final BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<GreenBean>();
		mapper.setStrict(true);
		mapper.setTargetType(GreenBean.class);
		// mapper.setDistanceLimit(0);
		final DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "blue", "green" };
		lineTokenizer.setNames(names);

		ExecutorService executorService = Executors.newFixedThreadPool(5);
		Collection<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < 10; i++) {
			Future<Boolean> result = executorService.submit(new Callable<Boolean>() {
				public Boolean call() throws Exception {
					for (int i = 0; i < 10; i++) {
						GreenBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("blue,green"));
						Assert.assertEquals("green", bean.getGreen());
					}
					return true;
				}
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
