/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemReader;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemWriter;
import org.springframework.batch.infrastructure.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.batch.infrastructure.item.amqp.builder.AmqpItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.jms.JmsItemReader;
import org.springframework.batch.infrastructure.item.jms.JmsItemWriter;
import org.springframework.batch.infrastructure.item.jms.builder.JmsItemReaderBuilder;
import org.springframework.batch.infrastructure.item.jms.builder.JmsItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.infrastructure.item.queue.BlockingQueueItemReader;
import org.springframework.batch.infrastructure.item.queue.BlockingQueueItemWriter;
import org.springframework.batch.infrastructure.item.queue.builder.BlockingQueueItemReaderBuilder;
import org.springframework.batch.infrastructure.item.queue.builder.BlockingQueueItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.AbstractFileItemWriter;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemStreamItemWriter;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemReader;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemWriter;
import org.springframework.batch.infrastructure.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.infrastructure.item.xml.builder.StaxEventItemWriterBuilder;

import java.util.Set;

/**
 * {@link RuntimeHintsRegistrar} for Spring Batch infrastructure module.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.2.2
 */
public class InfrastructureRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// reflection hints
		Set<Class<?>> classes = Set.of(
				// File IO APIs
				FlatFileItemReader.class, FlatFileItemReaderBuilder.class, FlatFileItemWriter.class,
				FlatFileItemWriterBuilder.class, JsonItemReader.class, JsonItemReaderBuilder.class,
				JsonFileItemWriter.class, JsonFileItemWriterBuilder.class, StaxEventItemReader.class,
				StaxEventItemReaderBuilder.class, StaxEventItemWriter.class, StaxEventItemWriterBuilder.class,

				// Database IO APIs
				JdbcCursorItemReader.class, JdbcCursorItemReaderBuilder.class, JdbcPagingItemReader.class,
				JdbcPagingItemReaderBuilder.class, JdbcBatchItemWriter.class, JdbcBatchItemWriterBuilder.class,
				JpaCursorItemReader.class, JpaCursorItemReaderBuilder.class, JpaPagingItemReader.class,
				JpaPagingItemReaderBuilder.class, JpaItemWriter.class, JpaItemWriterBuilder.class,

				// Queue IO APIs
				BlockingQueueItemReader.class, BlockingQueueItemReaderBuilder.class, BlockingQueueItemWriter.class,
				BlockingQueueItemWriterBuilder.class, JmsItemReader.class, JmsItemReaderBuilder.class,
				JmsItemWriter.class, JmsItemWriterBuilder.class, AmqpItemReader.class, AmqpItemReaderBuilder.class,
				AmqpItemWriter.class, AmqpItemWriterBuilder.class,

				// Support classes
				AbstractFileItemWriter.class, AbstractItemStreamItemWriter.class,
				AbstractItemCountingItemStreamItemReader.class, AbstractItemStreamItemReader.class,
				ItemStreamSupport.class);
		for (Class<?> type : classes) {
			hints.reflection().registerType(type, MemberCategory.values());
		}
	}

}
