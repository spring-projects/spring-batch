//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.springframework.batch.support;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.StringUtils;

public enum DatabaseType {
    DERBY("Apache Derby"),
    DB2("DB2"),
    DB2VSE("DB2VSE"),
    DB2ZOS("DB2ZOS"),
    DB2AS400("DB2AS400"),
    HSQL("HSQL Database Engine"),
    SQLSERVER("Microsoft SQL Server"),
    MYSQL("MySQL"),
    ORACLE("Oracle"),
    POSTGRES("PostgreSQL"),
    KINGBASE("KingbaseES"),
    SYBASE("Sybase"),
    H2("H2"),
    SQLITE("SQLite"),
    HANA("HDB"),
    MARIADB("MariaDB");

    private static final Map<String, DatabaseType> nameMap = new HashMap();
    private final String productName;

    private DatabaseType(String productName) {
        this.productName = productName;
    }

    public String getProductName() {
        return this.productName;
    }

    public static DatabaseType fromProductName(String productName) {
        if (!nameMap.containsKey(productName)) {
            throw new IllegalArgumentException("DatabaseType not found for product name: [" + productName + "]");
        } else {
            return (DatabaseType)nameMap.get(productName);
        }
    }

    public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
        String databaseProductName = (String)JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        if (StringUtils.hasText(databaseProductName) && databaseProductName.startsWith("DB2")) {
            String databaseProductVersion = (String)JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductVersion);
            if (databaseProductVersion.startsWith("ARI")) {
                databaseProductName = "DB2VSE";
            } else if (databaseProductVersion.startsWith("DSN")) {
                databaseProductName = "DB2ZOS";
            } else if (!databaseProductName.contains("AS") || !databaseProductVersion.startsWith("QSQ") && !databaseProductVersion.substring(databaseProductVersion.indexOf(86)).matches("V\\dR\\d[mM]\\d")) {
                databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
            } else {
                databaseProductName = "DB2AS400";
            }
        } else if (StringUtils.hasText(databaseProductName) && databaseProductName.startsWith("EnterpriseDB")) {
            databaseProductName = "PostgreSQL";
        } else {
            databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
        }

        return fromProductName(databaseProductName);
    }

    static {
        DatabaseType[] var0 = values();
        int var1 = var0.length;

        for(int var2 = 0; var2 < var1; ++var2) {
            DatabaseType type = var0[var2];
            nameMap.put(type.getProductName(), type);
        }

    }
}
