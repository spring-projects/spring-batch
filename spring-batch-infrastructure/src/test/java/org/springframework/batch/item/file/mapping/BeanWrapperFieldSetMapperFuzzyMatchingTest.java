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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.validation.BindException;

public class BeanWrapperFieldSetMapperFuzzyMatchingTest {

	@Test(expected = NotWritablePropertyException.class)
	public void testFuzzyMatchingWithKeyCandidateCollision() throws BindException {
		BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<GreenBean>();
		mapper.setStrict(true);
		mapper.setTargetType(GreenBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "brown", "green", "great", "groin", "braun" };
		lineTokenizer.setNames(names);
		GreenBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("brown,green,great,groin,braun"));
		Assert.assertEquals("green", bean.getGreen());
	}

	@Test
	public void testFuzzyMatchingWithLowerLimit() throws BindException {
		BeanWrapperFieldSetMapper<GreenBean> mapper = new BeanWrapperFieldSetMapper<GreenBean>();
		mapper.setDistanceLimit(0);
		mapper.setStrict(false);
		mapper.setTargetType(GreenBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "brown", "green", "great", "groin", "braun" };
		lineTokenizer.setNames(names);
		GreenBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("brown,green,great,groin,braun"));
		Assert.assertEquals("green", bean.getGreen());
	}

	@Test
	public void testFuzzyMatchingWithPropertyCollision() throws BindException {
		BeanWrapperFieldSetMapper<BlueBean> mapper = new BeanWrapperFieldSetMapper<BlueBean>();
		mapper.setStrict(true);
		mapper.setTargetType(BlueBean.class);
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		String[] names = { "blue" };
		lineTokenizer.setNames(names);
		BlueBean bean = mapper.mapFieldSet(lineTokenizer.tokenize("blue"));
		// An exact match always wins...
		Assert.assertEquals("blue", bean.getBlue());
		Assert.assertEquals(null, bean.getBleu());
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
