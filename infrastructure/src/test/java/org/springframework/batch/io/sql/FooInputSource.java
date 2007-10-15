package org.springframework.batch.io.sql;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.support.AbstractDrivingQueryInputSource;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

class FooInputSource implements InputSource, Restartable, DisposableBean, InitializingBean{

	AbstractDrivingQueryInputSource inputSource;
	FooDao fooDao = new SingleKeyFooDao();

	public FooInputSource(AbstractDrivingQueryInputSource inputSource, JdbcTemplate jdbcTemplate) {
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

	public RestartData getRestartData() {
		return inputSource.getRestartData();
	}

	public void restoreFrom(RestartData data) {
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
