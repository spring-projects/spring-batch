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

package org.springframework.batch.infrastructure.item.file.mapping;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.validation.BindException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanWrapperFieldSetMapperFuzzyMatchingTests {

	@Test
	void testFuzzyMatchingWithKeyCandidateCollision() {
		BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setStrict(true);
		mapper.setTargetType(GreenBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "brown", "green", "great", "groin", "braun" };
		lineTokenizer.setNames(names);
		assertThrows(NotWritablePropertyException.class,
				() -> mapper.mapFieldSet(lineTokenizer.tokenize("brown,green,great,groin,braun")));
	}

	@Test
	void testFuzzyMatchingWithLowerLimit() throws BindException {
		BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setDistanceLimit(0);
		mapper.setStrict(false);
		mapper.setTargetType(GreenBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "brown", "green", "great", "groin", "braun" };
		lineTokenizer.setNames(names);
		GreenBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("brown,green,great,groin,braun"));
		assertEquals("green", bean.getGreen());
	}

	@Test
	void testFuzzyMatchingWithPropertyCollision() throws BindException {
		BeanWrapperFieldSetMapper<BlueBean> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setStrict(true);
		mapper.setTargetType(BlueBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "blue" };
		lineTokenizer.setNames(names);
		BlueBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("blue"));
		// An exact match always wins...
		assertEquals("blue", bean.getBlue());
		assertNull(bean.getBleu());
	}

	public static class GreenBean {

		private String green;

		public String getGreen() {
			return green;
		}

		public void setGreen(String green) {
			this.green = green;
		}

	}

	public static class BlueBean {

		private String blue;

		private String bleu;

		public String getBleu() {
			return bleu;
		}

		public void setBleu(String bleu) {
			this.bleu = bleu;
		}

		public String getBlue() {
			return blue;
		}

		public void setBlue(String blue) {
			this.blue = blue;
		}

	}

}
