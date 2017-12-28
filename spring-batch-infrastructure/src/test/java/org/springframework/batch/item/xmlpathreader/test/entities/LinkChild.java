package org.springframework.batch.item.xmlpathreader.test.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;

@XmlPath(path = "child")
public class LinkChild {
	private static final Logger log = LoggerFactory.getLogger(LinkChild.class);

	private String name;

	private LinkChild child;

	private City city;

	private City second;

	private static int snr;

	private int nr;

	public LinkChild() {
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

	public LinkChild getChild() {
		return child;
	}

	@XmlPath(path = "child")
	public void setChild(LinkChild child) {
		log.debug(" Child " + this + " auf Child " + child);
		this.child = child;
	}

	public City getCity() {
		return city;
	}

	@XmlPath(path = "city")
	public void setCity(City city) {
		log.debug(" City " + this + " auf city " + city);
		this.city = city;
	}

	public City getSecond() {
		return second;
	}

	@XmlPath(path = "second")
	public void setSecond(City second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return "LinkChild [name=" + name + ", child=" + child + ", city=" + city + ", second=" + second + ", nr=" + nr
				+ "]";
	}

}
