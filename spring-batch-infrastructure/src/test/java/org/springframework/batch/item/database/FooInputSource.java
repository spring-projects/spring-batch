package org.springframework.batch.item.database;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

class FooItemReader implements ItemStream, ItemReader<Foo>, DisposableBean, InitializingBean {

	DrivingQueryItemReader<Foo> itemReader;

	public void setItemReader(DrivingQueryItemReader<Foo> itemReader) {
		this.itemReader = itemReader;
	}

	FooDao fooDao = new SingleKeyFooDao();

	public FooItemReader(DrivingQueryItemReader<Foo> inputSource, JdbcTemplate jdbcTemplate) {
		this.itemReader = inputSource;
		fooDao.setJdbcTemplate(jdbcTemplate);
	}

	public Foo read() {
		Object key = itemReader.read();
		if (key != null) {
			return fooDao.getFoo(key);
		}
		else {
			return null;
		}
	}

	public void update(ExecutionContext executionContext) {
		itemReader.update(executionContext);
	}

	public void destroy() throws Exception {
		itemReader.close(null);
	}

	public void setFooDao(FooDao fooDao) {
		this.fooDao = fooDao;
	}

	public void afterPropertiesSet() throws Exception {
	}

	public void open(ExecutionContext executionContext) {
		itemReader.open(executionContext);
	};

	public void close(ExecutionContext executionContext) {
		itemReader.close(executionContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.StreamContext)
	 */
	public void mark() {
		itemReader.mark();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.StreamContext)
	 */
	public void reset() {
		itemReader.reset();
	};
}
