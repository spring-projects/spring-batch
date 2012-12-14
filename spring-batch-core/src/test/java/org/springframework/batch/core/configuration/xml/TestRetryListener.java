package org.springframework.batch.core.configuration.xml;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

public class TestRetryListener extends AbstractTestComponent implements RetryListener {

	public <T> void close(RetryContext context, RetryCallback<T> callback,
			Throwable throwable) {
	}

	public <T> void onError(RetryContext context, RetryCallback<T> callback,
			Throwable throwable) {
	}

	public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
		executed = true;
		return true;
	}

}
