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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

@XmlPath(path = "child/deep")
public class DeepChild {
	private static final Logger log = LoggerFactory.getLogger(DeepChild.class);

	private String name;

	private DeepChild child;

	private DeepCity city;

	private DeepCity second;

	private static int snr;

	private int nr;

	public DeepChild() {
		super();
		snr++;
		nr = snr;
	}

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void setName(String name) {
		log.debug(" Childname auf " + name);
		this.name = name;
	}

	public DeepChild getChild() {
		return child;
	}

	@XmlPath(path = "child")
	public void setChild(DeepChild child) {
		log.debug(" Child " + this + " auf Child " + child);
		this.child = child;
	}

	public DeepCity getCity() {
		return city;
	}

	@XmlPath(path = "city")
	public void setCity(DeepCity city) {
		log.debug(" City " + this + " auf city " + city);
		this.city = city;
	}

	public DeepCity getSecond() {
		return second;
	}

	@XmlPath(path = "second")
	public void setSecond(DeepCity second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return "LinkChild [name=" + name + ", child=" + child + ", city=" + city + ", second=" + second + ", nr=" + nr
				+ "]";
	}

}
