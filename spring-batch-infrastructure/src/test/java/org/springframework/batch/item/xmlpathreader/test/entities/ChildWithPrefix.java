package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

@XmlPath(path = "xsi:child")
public class ChildWithPrefix {

	private String name;

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void setName(String name) {
		this.name = name;
	}

}
