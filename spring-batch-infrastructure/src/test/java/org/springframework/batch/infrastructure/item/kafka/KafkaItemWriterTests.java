/*
 * Copyright 2019-present the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.infrastructure.item.kafka;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.kafka.KafkaItemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaItemWriterTests {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private CompletableFuture<SendResult<String, String>> future;

	private KafkaItemKeyMapper itemKeyMapper;

	private KafkaItemWriter<String, String> writer;

	@BeforeEach
	void setUp() throws Exception {
		when(this.kafkaTemplate.getDefaultTopic()).thenReturn("defaultTopic");
		when(this.kafkaTemplate.sendDefault(any(), any())).thenReturn(this.future);
		this.itemKeyMapper = new KafkaItemKeyMapper();
		this.writer = new KafkaItemWriter<>(this.itemKeyMapper, this.kafkaTemplate);
		this.writer.setDelete(false);
		this.writer.setTimeout(10L);
		this.writer.afterPropertiesSet();
	}

	@Test
	void testWriteDoesNotRethrowPreviousAsyncFailure() throws Exception {
		when(this.kafkaTemplate.sendDefault("a", "a"))
			.thenReturn(CompletableFuture.failedFuture(new RuntimeException("send failed")));
		when(this.kafkaTemplate.sendDefault("b", "b")).thenReturn(CompletableFuture.completedFuture(null));

		assertThrows(ExecutionException.class, () -> this.writer.write(Chunk.of("a")));
		assertDoesNotThrow(() -> this.writer.write(Chunk.of("b")));
	}

	@Test
	void testWriteDoesNotCarryFutureAfterSynchronousSendFailure() throws Exception {
		when(this.kafkaTemplate.sendDefault("a", "a"))
			.thenReturn(CompletableFuture.failedFuture(new RuntimeException("prior async")));
		when(this.kafkaTemplate.sendDefault("boom", "boom")).thenThrow(new IllegalStateException("send failed"));
		when(this.kafkaTemplate.sendDefault("c", "c")).thenReturn(CompletableFuture.completedFuture(null));

		assertThrows(IllegalStateException.class, () -> this.writer.write(Chunk.of("a", "boom")));
		verify(this.kafkaTemplate, never()).flush();
		assertDoesNotThrow(() -> this.writer.write(Chunk.of("c")));
	}

	@Test
	void testWriteDoesNotCarryFutureAfterKafkaTemplateFlushFailure() throws Exception {
		when(this.kafkaTemplate.sendDefault("a", "a"))
			.thenReturn(CompletableFuture.failedFuture(new RuntimeException("prior async")));
		when(this.kafkaTemplate.sendDefault("b", "b")).thenReturn(CompletableFuture.completedFuture(null));
		doThrow(new IllegalStateException("flush failed")).doNothing().when(this.kafkaTemplate).flush();

		assertThrows(IllegalStateException.class, () -> this.writer.write(Chunk.of("a")));
		assertDoesNotThrow(() -> this.writer.write(Chunk.of("b")));
	}

	@Test
	void testWriteDoesNotCarryFutureAfterTimeout() throws Exception {
		when(this.kafkaTemplate.sendDefault("a", "a")).thenReturn(new CompletableFuture<>());
		when(this.kafkaTemplate.sendDefault("b", "b")).thenReturn(CompletableFuture.completedFuture(null));

		assertThrows(TimeoutException.class, () -> this.writer.write(Chunk.of("a")));
		assertDoesNotThrow(() -> this.writer.write(Chunk.of("b")));
	}

	@Test
	void testWriteRethrowsOriginalAsyncFailure() throws Exception {
		RuntimeException cause = new RuntimeException("send failed");
		when(this.kafkaTemplate.sendDefault("a", "a")).thenReturn(CompletableFuture.failedFuture(cause));

		ExecutionException exception = assertThrows(ExecutionException.class, () -> this.writer.write(Chunk.of("a")));
		assertSame(cause, exception.getCause());
	}

	@Test
	void testWriteRethrowsOriginalSynchronousFailure() throws Exception {
		IllegalStateException failure = new IllegalStateException("send failed");
		when(this.kafkaTemplate.sendDefault("boom", "boom")).thenThrow(failure);

		assertSame(failure, assertThrows(IllegalStateException.class, () -> this.writer.write(Chunk.of("boom"))));
	}

	@Test
	void testWriteRethrowsInterruptedException() throws Exception {
		InterruptedException failure = new InterruptedException("interrupted");
		when(this.future.get(10L, TimeUnit.MILLISECONDS)).thenThrow(failure).thenReturn(null);

		assertSame(failure, assertThrows(InterruptedException.class, () -> this.writer.write(Chunk.of("a"))));
		assertDoesNotThrow(() -> this.writer.write(Chunk.of("b")));
	}

	@Test
	void testBasicWrite() throws Exception {
		Chunk<String> chunk = Chunk.of("val1", "val2");

		this.writer.write(chunk);

		List<String> items = chunk.getItems();
		verify(this.kafkaTemplate).sendDefault(items.get(0), items.get(0));
		verify(this.kafkaTemplate).sendDefault(items.get(1), items.get(1));
		verify(this.kafkaTemplate).flush();
		verify(this.future, times(2)).get(10L, TimeUnit.MILLISECONDS);
	}

	@Test
	void testBasicDelete() throws Exception {
		Chunk<String> chunk = Chunk.of("val1", "val2");
		this.writer.setDelete(true);

		this.writer.write(chunk);

		List<String> items = chunk.getItems();
		verify(this.kafkaTemplate).sendDefault(items.get(0), null);
		verify(this.kafkaTemplate).sendDefault(items.get(1), null);
		verify(this.kafkaTemplate).flush();
		verify(this.future, times(2)).get(10L, TimeUnit.MILLISECONDS);
	}

	@Test
	void testKafkaTemplateCanBeReferencedFromSubclass() {
		KafkaItemWriter<String, String> kafkaItemWriter = new KafkaItemWriter<>(new KafkaItemKeyMapper(),
				this.kafkaTemplate) {
			@Override
			protected void writeKeyValue(String key, String value) {
				this.kafkaTemplate.sendDefault(key, value);
			}
		};
		kafkaItemWriter.writeKeyValue("k", "v");
		verify(this.kafkaTemplate).sendDefault("k", "v");
	}

	static class KafkaItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}

	}

}
