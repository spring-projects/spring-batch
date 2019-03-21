/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import java.math.BigInteger;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.support.ColumnMapItemPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link JdbcBatchItemWriter}.
 *
 * @author Michael Minella
 * @since 4.0
 * @see JdbcBatchItemWriter
 */
public class JdbcBatchItemWriterBuilder<T> {

	private boolean assertUpdates = true;

	private String sql;

	private ItemPreparedStatementSetter<T> itemPreparedStatementSetter;

	private ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider;

	private DataSource dataSource;

	private NamedParameterJdbcOperations namedParameterJdbcTemplate;

	private BigInteger mapped = new BigInteger("0");

	/**
	 * Configure the {@link DataSource} to be used.
	 *
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
	 * row in the database.  Defaults to true.
	 *
	 * @param assertUpdates boolean indicator
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setAssertUpdates(boolean)
	 */
	public JdbcBatchItemWriterBuilder<T> assertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;

		return this;
	}

	/**
	 * Set the SQL statement to be used for each item's updates.  This is a required
	 * field.
	 *
	 * @param sql SQL string
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setSql(String)
	 */
	public JdbcBatchItemWriterBuilder<T> sql(String sql) {
		this.sql = sql;

		return this;
	}

	/**
	 * Configures a {@link ItemPreparedStatementSetter} for use by the writer.  This
	 * should only be used if {@link #columnMapped()} isn't called.
	 *
	 * @param itemPreparedStatementSetter The {@link ItemPreparedStatementSetter}
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setItemPreparedStatementSetter(ItemPreparedStatementSetter)
	 */
	public JdbcBatchItemWriterBuilder<T> itemPreparedStatementSetter(ItemPreparedStatementSetter<T> itemPreparedStatementSetter) {
		this.itemPreparedStatementSetter = itemPreparedStatementSetter;

		return this;
	}

	/**
	 * Configures a {@link ItemSqlParameterSourceProvider} for use by the writer.  This
	 * should only be used if {@link #beanMapped()} isn't called.
	 *
	 * @param itemSqlParameterSourceProvider The {@link ItemSqlParameterSourceProvider}
	 * @return The current instance of the builder for chaining
	 * @see JdbcBatchItemWriter#setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider)
	 */
	public JdbcBatchItemWriterBuilder<T> itemSqlParameterSourceProvider(ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
		this.itemSqlParameterSourceProvider = itemSqlParameterSourceProvider;

		return this;
	}

	/**
	 * The {@link NamedParameterJdbcOperations} instance to use.  If one isn't provided,
	 * a {@link DataSource} is required.
	 *
	 * @param namedParameterJdbcOperations The template
	 * @return The current instance of the builder for chaining
	 */
	public JdbcBatchItemWriterBuilder<T> namedParametersJdbcTemplate(NamedParameterJdbcOperations namedParameterJdbcOperations) {
		this.namedParameterJdbcTemplate = namedParameterJdbcOperations;

		return this;
	}

	/**
	 * Creates a {@link ColumnMapItemPreparedStatementSetter} to be used as your
	 * {@link ItemPreparedStatementSetter}.
	 *
	 * NOTE: The item type for this {@link org.springframework.batch.item.ItemWriter} must
	 * be castable to <code>Map&lt;String,Object&gt;&gt;</code>.
	 *
	 * @return The current instance of the builder for chaining
	 * @see ColumnMapItemPreparedStatementSetter
	 */
	public JdbcBatchItemWriterBuilder<T> columnMapped() {
		this.mapped = this.mapped.setBit(0);

		return this;
	}

	/**
	 * Creates a {@link BeanPropertyItemSqlParameterSourceProvider} to be used as your
	 * {@link ItemSqlParameterSourceProvider}.
	 *
	 * @return The current instance of the builder for chaining
	 * @see BeanPropertyItemSqlParameterSourceProvider
	 */
	public JdbcBatchItemWriterBuilder<T> beanMapped() {
		this.mapped = this.mapped.setBit(1);

		return this;
	}

	/**
	 * Validates configuration and builds the {@link JdbcBatchItemWriter}.
	 *
	 * @return a {@link JdbcBatchItemWriter}
	 */
	@SuppressWarnings("unchecked")
	public JdbcBatchItemWriter<T> build() {
		Assert.state(this.dataSource != null || this.namedParameterJdbcTemplate != null,
				"Either a DataSource or a NamedParameterJdbcTemplate is required");

		Assert.notNull(this.sql, "A SQL statement is required");
		int mappedValue = this.mapped.intValue();
		Assert.state(mappedValue != 3,
				"Either an item can be mapped via db column or via bean spec, can't be both");

		JdbcBatchItemWriter<T> writer = new JdbcBatchItemWriter<>();
		writer.setSql(this.sql);
		writer.setAssertUpdates(this.assertUpdates);
		writer.setItemSqlParameterSourceProvider(this.itemSqlParameterSourceProvider);
		writer.setItemPreparedStatementSetter(this.itemPreparedStatementSetter);

		if(mappedValue == 1) {
			((JdbcBatchItemWriter<Map<String,Object>>)writer).setItemPreparedStatementSetter(new ColumnMapItemPreparedStatementSetter());
		} else if(mappedValue == 2) {
			writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		}

		if(this.dataSource != null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
		}

		writer.setJdbcTemplate(this.namedParameterJdbcTemplate);

		return writer;
	}
}
