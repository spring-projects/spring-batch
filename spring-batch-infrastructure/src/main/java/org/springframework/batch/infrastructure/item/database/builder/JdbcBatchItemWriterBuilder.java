/*
 * Copyright 2016-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.builder;

import java.math.BigInteger;
import java.util.Map;
import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.infrastructure.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.support.ColumnMapItemPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link JdbcBatchItemWriter}.
 *
 * @author Michael Minella
 * @author Stefano Cordio
 * @since 4.0
 * @see JdbcBatchItemWriter
 */
public class JdbcBatchItemWriterBuilder<T> {

	private boolean assertUpdates = true;

	private @Nullable String sql;

	private @Nullable ItemPreparedStatementSetter<T> itemPreparedStatementSetter;

	private @Nullable ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider;

	private @Nullable DataSource dataSource;

	private @Nullable NamedParameterJdbcOperations namedParameterJdbcTemplate;

	private BigInteger mapped = new BigInteger("0");

	/**
	 * Configure the {@link DataSource} to be used.
	 * @param dataSource the DataSource
	 * @return The current instance of the builder for chaining.
	 * @see JdbcBatchItemWriter#setDataSource(DataSource)
	 */
	public JdbcBatchItemWriterBuilder<T> dataSource(DataSource dataSource) {
		this.dataSource = dataSource;

		return this;
	}

	/**
	 * If set to true, confirms that every insert results in the update of at least one
	 * row in the database. Defaults to true.
	 * @param assertUpdates boolean indicator
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setAssertUpdates(boolean)
	 */
	public JdbcBatchItemWriterBuilder<T> assertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;

		return this;
	}

	/**
	 * Set the SQL statement to be used for each item's updates. This is a required field.
	 * @param sql SQL string
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setSql(String)
	 */
	public JdbcBatchItemWriterBuilder<T> sql(String sql) {
		this.sql = sql;

		return this;
	}

	/**
	 * Configures a {@link ItemPreparedStatementSetter} for use by the writer. This should
	 * only be used if {@link #columnMapped()} isn't called.
	 * @param itemPreparedStatementSetter The {@link ItemPreparedStatementSetter}
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setItemPreparedStatementSetter(ItemPreparedStatementSetter)
	 */
	public JdbcBatchItemWriterBuilder<T> itemPreparedStatementSetter(
			ItemPreparedStatementSetter<T> itemPreparedStatementSetter) {
		this.itemPreparedStatementSetter = itemPreparedStatementSetter;

		return this;
	}

	/**
	 * Configures a {@link ItemSqlParameterSourceProvider} for use by the writer. This
	 * should only be used if {@link #beanMapped()} isn't called.
	 * @param itemSqlParameterSourceProvider The {@link ItemSqlParameterSourceProvider}
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider)
	 */
	public JdbcBatchItemWriterBuilder<T> itemSqlParameterSourceProvider(
			ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
		this.itemSqlParameterSourceProvider = itemSqlParameterSourceProvider;

		return this;
	}

	/**
	 * The {@link NamedParameterJdbcOperations} instance to use. If one isn't provided, a
	 * {@link DataSource} is required.
	 * @param namedParameterJdbcOperations The template
	 * @return The current instance of the builder for chaining
	 */
	public JdbcBatchItemWriterBuilder<T> namedParametersJdbcTemplate(
			NamedParameterJdbcOperations namedParameterJdbcOperations) {
		this.namedParameterJdbcTemplate = namedParameterJdbcOperations;

		return this;
	}

	/**
	 * Creates a {@link ColumnMapItemPreparedStatementSetter} to be used as your
	 * {@link ItemPreparedStatementSetter}.
	 * <p>
	 * NOTE: The item type for this {@link ItemWriter} must be castable to
	 * <code>Map&lt;String,Object&gt;&gt;</code>.
	 * @return A stage that prevents calling beanMapped()
	 * @see ColumnMapItemPreparedStatementSetter
	 */
	public ColumnMappedStage<T> columnMapped() {
		this.mapped = this.mapped.setBit(0);

		return new ColumnMappedStageImpl<>(this);
	}

	/**
	 * Creates a {@link BeanPropertyItemSqlParameterSourceProvider} to be used as your
	 * {@link ItemSqlParameterSourceProvider}.
	 * @return A stage that prevents calling columnMapped()
	 * @see BeanPropertyItemSqlParameterSourceProvider
	 */
	public BeanMappedStage<T> beanMapped() {
		this.mapped = this.mapped.setBit(1);

		return new BeanMappedStageImpl<>(this);
	}

	/**
	 * Validates configuration and builds the {@link JdbcBatchItemWriter}.
	 * @return a {@link JdbcBatchItemWriter}
	 */
	@SuppressWarnings("unchecked")
	public JdbcBatchItemWriter<T> build() {
		Assert.state(this.dataSource != null || this.namedParameterJdbcTemplate != null,
				"Either a DataSource or a NamedParameterJdbcTemplate is required");

		Assert.notNull(this.sql, "A SQL statement is required");
		int mappedValue = this.mapped.intValue();
		Assert.state(mappedValue != 3, "Either an item can be mapped via db column or via bean spec, can't be both");

		JdbcBatchItemWriter<T> writer = new JdbcBatchItemWriter<>();
		writer.setSql(this.sql);
		writer.setAssertUpdates(this.assertUpdates);
		if (this.itemSqlParameterSourceProvider != null) {
			writer.setItemSqlParameterSourceProvider(this.itemSqlParameterSourceProvider);
		}
		if (this.itemPreparedStatementSetter != null) {
			writer.setItemPreparedStatementSetter(this.itemPreparedStatementSetter);
		}

		if (mappedValue == 1) {
			((JdbcBatchItemWriter<Map<String, Object>>) writer)
				.setItemPreparedStatementSetter(new ColumnMapItemPreparedStatementSetter());
		}
		else if (mappedValue == 2) {
			writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		}

		if (this.dataSource != null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
		}

		if (this.namedParameterJdbcTemplate != null) {
			writer.setJdbcTemplate(this.namedParameterJdbcTemplate);
		}

		writer.afterPropertiesSet();

		return writer;
	}

	/**
	 * Stage reached once {@link #columnMapped()} has been called. It exposes the common
	 * configuration methods of {@link JdbcBatchItemWriterBuilder}, but not
	 * {@link #beanMapped()}, preventing an item from being mapped both via db column and
	 * via bean spec on the same builder instance.
	 *
	 * @param <T> the type of the item to write
	 * @since 6.1
	 */
	public interface ColumnMappedStage<T> {

		/**
		 * Configure the {@link DataSource} to be used.
		 * @param dataSource the DataSource
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setDataSource(DataSource)
		 */
		ColumnMappedStage<T> dataSource(DataSource dataSource);

		/**
		 * If set to true, confirms that every insert results in the update of at least
		 * one row in the database. Defaults to true.
		 * @param assertUpdates boolean indicator
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setAssertUpdates(boolean)
		 */
		ColumnMappedStage<T> assertUpdates(boolean assertUpdates);

		/**
		 * Set the SQL statement to be used for each item's updates. This is a required
		 * field.
		 * @param sql SQL string
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setSql(String)
		 */
		ColumnMappedStage<T> sql(String sql);

		/**
		 * Configures a {@link ItemPreparedStatementSetter} for use by the writer.
		 * @param itemPreparedStatementSetter The {@link ItemPreparedStatementSetter}
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setItemPreparedStatementSetter(ItemPreparedStatementSetter)
		 */
		ColumnMappedStage<T> itemPreparedStatementSetter(ItemPreparedStatementSetter<T> itemPreparedStatementSetter);

		/**
		 * Configures a {@link ItemSqlParameterSourceProvider} for use by the writer.
		 * @param itemSqlParameterSourceProvider The
		 * {@link ItemSqlParameterSourceProvider}
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider)
		 */
		ColumnMappedStage<T> itemSqlParameterSourceProvider(
				ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider);

		/**
		 * The {@link NamedParameterJdbcOperations} instance to use. If one isn't
		 * provided, a {@link DataSource} is required.
		 * @param namedParameterJdbcOperations The template
		 * @return The current instance of the stage.
		 */
		ColumnMappedStage<T> namedParametersJdbcTemplate(NamedParameterJdbcOperations namedParameterJdbcOperations);

		/**
		 * Validates configuration and builds the {@link JdbcBatchItemWriter}.
		 * @return a {@link JdbcBatchItemWriter}
		 */
		JdbcBatchItemWriter<T> build();

	}

	/**
	 * Stage reached once {@link #beanMapped()} has been called. It exposes the common
	 * configuration methods of {@link JdbcBatchItemWriterBuilder}, but not
	 * {@link #columnMapped()}, preventing an item from being mapped both via db column
	 * and via bean spec on the same builder instance.
	 *
	 * @param <T> the type of the item to write
	 * @since 6.1
	 */
	public interface BeanMappedStage<T> {

		/**
		 * Configure the {@link DataSource} to be used.
		 * @param dataSource the DataSource
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setDataSource(DataSource)
		 */
		BeanMappedStage<T> dataSource(DataSource dataSource);

		/**
		 * If set to true, confirms that every insert results in the update of at least
		 * one row in the database. Defaults to true.
		 * @param assertUpdates boolean indicator
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setAssertUpdates(boolean)
		 */
		BeanMappedStage<T> assertUpdates(boolean assertUpdates);

		/**
		 * Set the SQL statement to be used for each item's updates. This is a required
		 * field.
		 * @param sql SQL string
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setSql(String)
		 */
		BeanMappedStage<T> sql(String sql);

		/**
		 * Configures a {@link ItemPreparedStatementSetter} for use by the writer.
		 * @param itemPreparedStatementSetter The {@link ItemPreparedStatementSetter}
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setItemPreparedStatementSetter(ItemPreparedStatementSetter)
		 */
		BeanMappedStage<T> itemPreparedStatementSetter(ItemPreparedStatementSetter<T> itemPreparedStatementSetter);

		/**
		 * Configures a {@link ItemSqlParameterSourceProvider} for use by the writer.
		 * @param itemSqlParameterSourceProvider The
		 * {@link ItemSqlParameterSourceProvider}
		 * @return The current instance of the stage.
		 * @see JdbcBatchItemWriter#setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider)
		 */
		BeanMappedStage<T> itemSqlParameterSourceProvider(
				ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider);

		/**
		 * The {@link NamedParameterJdbcOperations} instance to use. If one isn't
		 * provided, a {@link DataSource} is required.
		 * @param namedParameterJdbcOperations The template
		 * @return The current instance of the stage.
		 */
		BeanMappedStage<T> namedParametersJdbcTemplate(NamedParameterJdbcOperations namedParameterJdbcOperations);

		/**
		 * Validates configuration and builds the {@link JdbcBatchItemWriter}.
		 * @return a {@link JdbcBatchItemWriter}
		 */
		JdbcBatchItemWriter<T> build();

	}

	private static class ColumnMappedStageImpl<T> implements ColumnMappedStage<T> {

		private final JdbcBatchItemWriterBuilder<T> parent;

		private ColumnMappedStageImpl(JdbcBatchItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		@Override
		public ColumnMappedStage<T> dataSource(DataSource dataSource) {
			this.parent.dataSource(dataSource);
			return this;
		}

		@Override
		public ColumnMappedStage<T> assertUpdates(boolean assertUpdates) {
			this.parent.assertUpdates(assertUpdates);
			return this;
		}

		@Override
		public ColumnMappedStage<T> sql(String sql) {
			this.parent.sql(sql);
			return this;
		}

		@Override
		public ColumnMappedStage<T> itemPreparedStatementSetter(
				ItemPreparedStatementSetter<T> itemPreparedStatementSetter) {
			this.parent.itemPreparedStatementSetter(itemPreparedStatementSetter);
			return this;
		}

		@Override
		public ColumnMappedStage<T> itemSqlParameterSourceProvider(
				ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
			this.parent.itemSqlParameterSourceProvider(itemSqlParameterSourceProvider);
			return this;
		}

		@Override
		public ColumnMappedStage<T> namedParametersJdbcTemplate(
				NamedParameterJdbcOperations namedParameterJdbcOperations) {
			this.parent.namedParametersJdbcTemplate(namedParameterJdbcOperations);
			return this;
		}

		@Override
		public JdbcBatchItemWriter<T> build() {
			return this.parent.build();
		}

	}

	private static class BeanMappedStageImpl<T> implements BeanMappedStage<T> {

		private final JdbcBatchItemWriterBuilder<T> parent;

		private BeanMappedStageImpl(JdbcBatchItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		@Override
		public BeanMappedStage<T> dataSource(DataSource dataSource) {
			this.parent.dataSource(dataSource);
			return this;
		}

		@Override
		public BeanMappedStage<T> assertUpdates(boolean assertUpdates) {
			this.parent.assertUpdates(assertUpdates);
			return this;
		}

		@Override
		public BeanMappedStage<T> sql(String sql) {
			this.parent.sql(sql);
			return this;
		}

		@Override
		public BeanMappedStage<T> itemPreparedStatementSetter(
				ItemPreparedStatementSetter<T> itemPreparedStatementSetter) {
			this.parent.itemPreparedStatementSetter(itemPreparedStatementSetter);
			return this;
		}

		@Override
		public BeanMappedStage<T> itemSqlParameterSourceProvider(
				ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
			this.parent.itemSqlParameterSourceProvider(itemSqlParameterSourceProvider);
			return this;
		}

		@Override
		public BeanMappedStage<T> namedParametersJdbcTemplate(
				NamedParameterJdbcOperations namedParameterJdbcOperations) {
			this.parent.namedParametersJdbcTemplate(namedParameterJdbcOperations);
			return this;
		}

		@Override
		public JdbcBatchItemWriter<T> build() {
			return this.parent.build();
		}

	}

}
