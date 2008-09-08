package org.springframework.batch.core.repository.support;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.XStream;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author trisberg
 */
public class XStreamExecutionContextStringSerializer implements ExecutionContextStringSerializer, InitializingBean {

	private ReflectionProvider reflectionProvider = null;

	private HierarchicalStreamDriver hierarchicalStreamDriver;

	private XStream xstream;

	public String serialize(Map context) {
		return xstream.toXML(context);
	}

	public Map deserialize(String context) {
		return (Map) xstream.fromXML(context);
	}

	public void setReflectionProvider(ReflectionProvider reflectionProvider) {
		this.reflectionProvider = reflectionProvider;
	}

	public void setHierarchicalStreamDriver(HierarchicalStreamDriver hierarchicalStreamDriver) {
		this.hierarchicalStreamDriver = hierarchicalStreamDriver;
	}

	public void afterPropertiesSet() throws Exception {
		init();
	}

	public void init() throws Exception {
		if (hierarchicalStreamDriver == null) {
			this.hierarchicalStreamDriver = new JettisonMappedXmlDriver();
		}
		if (reflectionProvider == null) {
			xstream =  new XStream(hierarchicalStreamDriver);
		}
		else {
			xstream = new XStream(reflectionProvider, hierarchicalStreamDriver);
		}
	}
}
