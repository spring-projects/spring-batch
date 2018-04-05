/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

@XmlPath(path = "child")
public class Child {

	private String name;

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void setName(String name) {
		this.name = name;
	}

}
