package org.springframework.batch.test.jmx;

import org.springframework.context.ApplicationEvent;

/**
 * @author Dave Syer
 * 
 */
@SuppressWarnings("serial")
public class SimpleMessageApplicationEvent extends ApplicationEvent {

	private String message;

	public SimpleMessageApplicationEvent(Object source, String message) {
		super(source);
		this.message = message;
	}
	
	/* (non-Javadoc)
	 * @see java.util.EventObject#toString()
	 */
	public String toString() {
		return "message=["+message+"], " + super.toString();
	}

}