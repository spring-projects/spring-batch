//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.springframework.batch.item.database.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SqlPagingQueryProviderFactoryBean implements FactoryBean<PagingQueryProvider> {
    private DataSource dataSource;
    private String databaseType;
    private String fromClause;
    private String whereClause;
    private String selectClause;
    private String groupClause;
    private Map<String, Order> sortKeys;
    private final Map<DatabaseType, AbstractSqlPagingQueryProvider> providers = new HashMap();

    public SqlPagingQueryProviderFactoryBean() {
        this.providers.put(DatabaseType.DB2, new Db2PagingQueryProvider());
        this.providers.put(DatabaseType.DB2VSE, new Db2PagingQueryProvider());
        this.providers.put(DatabaseType.DB2ZOS, new Db2PagingQueryProvider());
        this.providers.put(DatabaseType.DB2AS400, new Db2PagingQueryProvider());
        this.providers.put(DatabaseType.DERBY, new DerbyPagingQueryProvider());
        this.providers.put(DatabaseType.HSQL, new HsqlPagingQueryProvider());
        this.providers.put(DatabaseType.H2, new H2PagingQueryProvider());
        this.providers.put(DatabaseType.HANA, new HanaPagingQueryProvider());
        this.providers.put(DatabaseType.MYSQL, new MySqlPagingQueryProvider());
        this.providers.put(DatabaseType.MARIADB, new MariaDBPagingQueryProvider());
        this.providers.put(DatabaseType.ORACLE, new OraclePagingQueryProvider());
        this.providers.put(DatabaseType.POSTGRES, new PostgresPagingQueryProvider());
        this.providers.put(DatabaseType.KINGBASE, new PostgresPagingQueryProvider());
        this.providers.put(DatabaseType.SQLITE, new SqlitePagingQueryProvider());
        this.providers.put(DatabaseType.SQLSERVER, new SqlServerPagingQueryProvider());
        this.providers.put(DatabaseType.SYBASE, new SybasePagingQueryProvider());
    }

    public void setGroupClause(String groupClause) {
        this.groupClause = groupClause;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setFromClause(String fromClause) {
        this.fromClause = fromClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public void setSelectClause(String selectClause) {
        this.selectClause = selectClause;
    }

    public void setSortKeys(Map<String, Order> sortKeys) {
        this.sortKeys = sortKeys;
    }

    public void setSortKey(String key) {
        Assert.doesNotContain(key, ",", "String setter is valid for a single ASC key only");
        Map<String, Order> keys = new LinkedHashMap();
        keys.put(key, Order.ASCENDING);
        this.sortKeys = keys;
    }

    public PagingQueryProvider getObject() throws Exception {
        DatabaseType type;
        try {
            type = this.databaseType != null ? DatabaseType.valueOf(this.databaseType.toUpperCase()) : DatabaseType.fromMetaData(this.dataSource);
        } catch (MetaDataAccessException var3) {
            throw new IllegalArgumentException("Could not inspect meta data for database type.  You have to supply it explicitly.", var3);
        }

        AbstractSqlPagingQueryProvider provider = (AbstractSqlPagingQueryProvider)this.providers.get(type);
        Assert.state(provider != null, "Should not happen: missing PagingQueryProvider for DatabaseType=" + type);
        provider.setFromClause(this.fromClause);
        provider.setWhereClause(this.whereClause);
        provider.setSortKeys(this.sortKeys);
        if (StringUtils.hasText(this.selectClause)) {
            provider.setSelectClause(this.selectClause);
        }

        if (StringUtils.hasText(this.groupClause)) {
            provider.setGroupClause(this.groupClause);
        }

        provider.init(this.dataSource);
        return provider;
    }

    public Class<PagingQueryProvider> getObjectType() {
        return PagingQueryProvider.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
