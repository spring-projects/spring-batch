package org.springframework.batch.core.test.infinite_loop_on_retry;

@SuppressWarnings("serial")
public class TestException extends Exception {

	public TestException(String message) {
		super(message);
	}

}
