package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryListener;

public class TestRetryListener extends AbstractTestComponent implements RetryListener {

	public <T> void close(RetryContext context, RetryCallback<T> callback,
			Throwable throwable) {
	}

	public <T> void onError(RetryContext context, RetryCallback<T> callback,
			Throwable throwable) {
	}

	public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
		System.out.println("RETRY RETRY RETRY RETRY");
		executed = true;
		return true;
	}

}
