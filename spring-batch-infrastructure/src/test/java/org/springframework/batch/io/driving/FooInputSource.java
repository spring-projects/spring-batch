package org.springframework.batch.io.driving;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.reader.AbstractItemReader;
import org.springframework.batch.stream.ItemStream;
import org.springframework.batch.stream.StreamContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

class FooItemReader extends AbstractItemReader implements ItemReader, ItemStream, DisposableBean, InitializingBean{

	DrivingQueryItemReader inputSource;
	FooDao fooDao = new SingleKeyFooDao();

	public FooItemReader(DrivingQueryItemReader inputSource, JdbcTemplate jdbcTemplate) {
		this.inputSource = inputSource;
		fooDao.setJdbcTemplate(jdbcTemplate);
	}

	public Object read() {
		Object key = inputSource.read();
		if(key != null){
			return fooDao.getFoo(key);
		}else{
			return null;
		}
	}

	public StreamContext getRestartData() {
		return inputSource.getRestartData();
	}

	public void restoreFrom(StreamContext data) {
		inputSource.restoreFrom(data);
	}

	public void destroy() throws Exception {
		inputSource.destroy();
	}

	public void setFooDao(FooDao fooDao) {
		this.fooDao = fooDao;
	}

	public void afterPropertiesSet() throws Exception {
	};
}
