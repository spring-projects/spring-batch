package org.springframework.batch.item.database;

import org.springframework.batch.item.support.AbstractItemReaderItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link org.springframework.batch.item.ItemReader} for reading database records using JDBC in a paging
 * fashion.
 *
 * It executes the SQL built from values specified for {@link #setSelectClause(String)} (String)},
 * {@link #setFromClause(String)} (String)} and {@link #setWhereClause(String)} (String)} to retrieve requested data.
 * The query is executed using paged requests of a size specified in {@link #setPageSize(int)}.  Additional pages
 * are requested when needed as {@link #read()} method is called, returning an object corresponding to current position.
 *
 * The performance of the paging depends on the database specific features available to limit the number of returned rows.
 *
 * Setting a fairly large page size and using a commit interval that matches the page size should provide
 * better performance.
 *
 * The implementation is *not* thread-safe.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class JdbcPagingItemReader<T> extends AbstractItemReaderItemStream<T> implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private DataSource dataSource;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private ParameterizedRowMapper<T> parameterizedRowMapper;

	private String databaseProductName;

	private String selectClause;

	private String fromClause;

	private String whereClause;

	private String sortKey;

	private String orderClause;

	private String firstPageSql;

	private String remainingPagesSql;

	private boolean initialized = false;

	private int current = 0;

	private int page = 0;

	private int pageSize = 10;

	private Object startAfterValue;

	private List<T> results;

	public JdbcPagingItemReader() {
		setName(ClassUtils.getShortName(JdbcPagingItemReader.class));
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @param selectClause SELECT clause part of SQL query string
	 */
	public void setSelectClause(String selectClause) {
		String keyWord = "select ";
		String temp = selectClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.selectClause = temp.substring(keyWord.length());
		}
		else {
			this.selectClause = temp;
		}
	}

	/**
	 * @param fromClause FROM clause part of SQL query string
	 */
	public void setFromClause(String fromClause) {
		String keyWord = "from ";
		String temp = fromClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.fromClause = temp.substring(keyWord.length());
		}
		else {
			this.fromClause = temp;
		}
	}

	/**
	 * @param whereClause WHERE clause part of SQL query string
	 */
	public void setWhereClause(String whereClause) {
		String keyWord = "where ";
		String temp = whereClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.whereClause = temp.substring(keyWord.length());
		}
		else {
			this.whereClause = temp;
		}
	}

	/**
	 * @param sortKey key to use to sort and limit page content
	 */
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	/**
	 * The number of rows to retreive at a time.
	 *
	 * @param pageSize the number of rows to fetch per page
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * The row mapper implementation to be used by this reader
	 *
	 * @param parameterizedRowMapper a {@link org.springframework.jdbc.core.simple.ParameterizedRowMapper} implementation
	 */
	public void setParameterizedRowMapper(ParameterizedRowMapper<T> parameterizedRowMapper) {
		this.parameterizedRowMapper = parameterizedRowMapper;
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource);
		Assert.hasLength(selectClause, "selectClause must be specified");
		Assert.hasLength(fromClause, "fromClause must be specified");
		Assert.hasLength(sortKey, "sortKey must be specified");
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setMaxRows(pageSize);
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(jdbcTemplate);
		initializeSqlStatements();
	}

	private void initializeSqlStatements() throws MetaDataAccessException {
		this.databaseProductName = JdbcUtils.commonDatabaseName(
				JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName").toString());
		String topClause = "";
		String limitCondition = "";
		String limitClause = "";
		if ("DB2".equals(databaseProductName)) {
			limitClause = " FETCH FIRST " + pageSize + " ROWS ONLY";
		}
		else if ("Oracle".equals(databaseProductName)) {
			limitCondition = "ROWNUM <= " + pageSize;
		}
		else if ("MySQL".equals(databaseProductName) || "PostgreSQL".equals(databaseProductName)) {
			limitClause = " LIMIT " + pageSize;
		}
		else if ("Microsoft SQL Server".equals(databaseProductName) || "Sybase".equals(databaseProductName) ||
				"HSQL Database Engine".equals(databaseProductName)) {
			topClause = "TOP " + pageSize + " ";
		}
		else if ("Apache Derby".equals(databaseProductName)) {
			String version = JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseVersion").toString();
			if ("10.4.1.3".compareTo(version) > 0) {
				throw new InvalidDataAccessResourceUsageException(databaseProductName + " version " + version + " is not supported");
			}
			// Derby doesn't support TOP or LIMIT -- maxRows will limit the rows retrieved
		}
		else {
			throw new InvalidDataAccessResourceUsageException(databaseProductName + " is not a supported database");
		}
		this.orderClause = " ORDER BY SORT_KEY";
		this.firstPageSql = "SELECT " + topClause + selectClause + ", " + sortKey + " AS SORT_KEY" +
				" FROM " + fromClause +
				(whereClause == null ? "" : " WHERE " + whereClause) +
				(limitCondition.length() == 0 ? "" : (whereClause == null ? " WHERE " : " AND ") + limitCondition) + 
				orderClause + limitClause;
		this.remainingPagesSql = "SELECT " + topClause + selectClause + ", " + sortKey + " AS SORT_KEY" +
				" FROM " + fromClause + " WHERE " + sortKey + " > ?" +
				(whereClause == null ? "" : " AND " + whereClause) +
				(limitCondition.length() == 0 ? "" : " AND " + limitCondition) + 
				orderClause + limitClause;
	}


	@Override
	@SuppressWarnings("unchecked")
	protected T doRead() throws Exception {

		if (results == null || current >= pageSize) {

			if (results == null) {
				results = new ArrayList();
			}
			else {
				results.clear();
			}

			if (page == 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("SQL used for reading first page: [" + firstPageSql + "]");
				}
				simpleJdbcTemplate.getJdbcOperations().query(firstPageSql,
						new RowCallbackHandler() {
							public void processRow(ResultSet rs) throws SQLException {
								startAfterValue = rs.getObject(1);
								results.add(parameterizedRowMapper.mapRow(rs, results.size()));
							}
						});
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("SQL used for reading remaining pages: [" + remainingPagesSql + "]");
				}
				simpleJdbcTemplate.getJdbcOperations().query(remainingPagesSql,
						new Object[] {startAfterValue},
						new RowCallbackHandler() {
							public void processRow(ResultSet rs) throws SQLException {
								startAfterValue = rs.getObject(1);
								results.add(parameterizedRowMapper.mapRow(rs, results.size()));
							}
						});
			}

			if (current >= pageSize) {
				current = 0;
			}
			page++;
		}

		if (current < results.size()) {
			return results.get(current++);
		}
		else {
			return null;
		}

	}

	@Override
	protected void doOpen() throws Exception {

		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");

		initialized = true;

	}

	@Override
	protected void doClose() throws Exception {

		initialized = false;

	}


	@Override
	protected void jumpToItem(int itemIndex) throws Exception {

		page = itemIndex / pageSize;
		current = itemIndex % pageSize;

		int offset = (page * pageSize) - 1;
		int lastRowNum = (page * pageSize);

		logger.debug("Jumping to page " + page + " and index " + current);

		if (page > 0) {

			String windowClause = "";
			String topClause = "";
			String limitClause = "";
			if ("DB2".equals(databaseProductName) || "Oracle".equals(databaseProductName) ||
					"Microsoft SQL Server".equals(databaseProductName) || "Sybase".equals(databaseProductName) ||
					"Apache Derby".equals(databaseProductName)) {
				windowClause = "ROW_NUMBER() OVER (ORDER BY " + sortKey + " ASC) AS ROW_NUMBER";
			}
			else if ("HSQL Database Engine".equals(databaseProductName)) {
				topClause = "LIMIT " + offset + " 1 ";
			}
			else if ("MySQL".equals(databaseProductName) || "PostgreSQL".equals(databaseProductName) ||
					"HSQL Database Engine".equals(databaseProductName)) {
				limitClause = " LIMIT 1 OFFSET " + offset;
			}

			String jumpToItemSql =
					(windowClause.length() > 0 ? "SELECT * FROM ( " : "") +
					"SELECT " + (topClause.length() > 0 ? topClause : "") + sortKey + " AS SORT_KEY" +
					(windowClause.length() > 0 ? ", " + windowClause : "") +
					" FROM " + fromClause + (whereClause == null ? "" : " WHERE " + whereClause) +
					(windowClause.length() > 0 ? ") WHERE ROW_NUMBER = " + lastRowNum : orderClause + limitClause);

			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for jumping: [" + jumpToItemSql + "]");
			}

			startAfterValue = simpleJdbcTemplate.getJdbcOperations().queryForObject(jumpToItemSql,
					new RowMapper() {
						public Object mapRow(ResultSet rs, int i) throws SQLException {
							 return rs.getObject(1);
						}
					});

		}

	}

}
