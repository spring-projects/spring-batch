package org.springframework.batch.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * <p>
 * Util class based off {@link org.springframework.test.jdbc.SimpleJdbcTestUtils} but for JdbcTemplate
 * rather than the deprecated SimpleJdbcTemplate.
 *
 * This class should be removed when Batch uses Spring 3.2 - see:
 * https://jira.springsource.org/browse/SPR-9235
 * </p>
 */
public final class JdbcTestUtils {
    private static final Log LOG = LogFactory.getLog(JdbcTestUtils.class);

    private JdbcTestUtils() {
    }

    /**
     * Count the rows in the given table.
     * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
     * @param tableName table name to count rows in
     * @return the number of rows in the table
     */
    public static int countRowsInTable(JdbcOperations jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForInt("SELECT COUNT(0) FROM " + tableName);
    }

    /**
     * Delete all rows from the specified tables.
     * @param jdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
     * @param tableNames the names of the tables from which to delete
     * @return the total number of rows deleted from all specified tables
     */
    public static int deleteFromTables(JdbcOperations jdbcTemplate, String... tableNames) {
        int totalRowCount = 0;
        for (String tableName : tableNames) {
            int rowCount = jdbcTemplate.update("DELETE FROM " + tableName);
            totalRowCount += rowCount;
            if (LOG.isInfoEnabled()) {
                LOG.info("Deleted " + rowCount + " rows from table " + tableName);
            }
        }
        return totalRowCount;
    }
}
