/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.json.domain.Trade;

/**
 * @author Mahmoud Ben Hassine
 */
public class GsonJsonItemWriterFunctionalTests extends JsonItemWriterFunctionalTests {

	@Override
	protected LineAggregator<Trade> getLineAggregator() {
		return new GsonLineAggregator<>();
	}

	@Override
	protected LineAggregator<Trade> getLineAggregatorWithPrettyPrint() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		GsonLineAggregator<Trade> lineAggregator = new GsonLineAggregator<>();
		lineAggregator.setGson(gson);
		return lineAggregator;
	}

	@Override
	protected String getExpectedPrettyPrintedFile() {
		return "expected-trades-gson-pretty-print.json";
	}

}
