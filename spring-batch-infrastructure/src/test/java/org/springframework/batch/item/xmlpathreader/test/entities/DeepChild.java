package org.springframework.batch.item.xmlpathreader.test.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

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
