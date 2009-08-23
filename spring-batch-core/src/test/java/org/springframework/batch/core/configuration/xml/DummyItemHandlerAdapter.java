package org.springframework.batch.core.configuration.xml;

/**
 * @author Dan Garrette
 * @since 2.1
 */
public class DummyItemHandlerAdapter {

	public Object dummyRead() {
		return null;
	}

	public Object dummyProcess(Object o) {
		return null;
	}

	public void dummyWrite(Object o) {
	}

}
