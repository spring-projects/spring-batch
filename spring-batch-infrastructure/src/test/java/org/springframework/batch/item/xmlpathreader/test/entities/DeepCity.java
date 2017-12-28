package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

@XmlPath(path = "city/deep")
public class DeepCity {

	private String name;

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void setName(String name) {
		this.name = name;

	}

	@Override
	public String toString() {
		return "City [name=" + name + "]";
	}

}
