/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.lang.Nullable;

public class TestReader extends AbstractTestComponent implements ItemStreamReader<String> {

	private boolean opened = false;

	List<String> items = null;

	{
		List<String> l = new ArrayList<>();
		l.add("Item *** 1 ***");
		l.add("Item *** 2 ***");
		this.items = Collections.synchronizedList(l);
	}

	public boolean isOpened() {
		return opened;
	}

	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	@Nullable
	@Override
	public String read() throws Exception, UnexpectedInputException, ParseException {
		executed = true;
		synchronized (items) {
			if (items.size() > 0) {
				String item = items.remove(0);
				return item;
			}
		}
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public void open(ExecutionContext executionContext) {
		opened = true;
	}

	@Override
	public void update(ExecutionContext executionContext) {
	}

}
