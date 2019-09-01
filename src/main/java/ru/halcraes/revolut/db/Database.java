package ru.halcraes.revolut.db;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Database implements AutoCloseable {
    private final JdbcDataSource dataSource;

    private Database(JdbcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Database initialize() {
        var dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:accounts;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        updateSchema(dataSource);

        return new Database(dataSource);
    }

    private static void updateSchema(JdbcDataSource dataSource) {
        try {
            var conn = new JdbcConnection(dataSource.getConnection());
            conn.setAutoCommit(false);
            try {
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(conn);
                var liquibase = new Liquibase("ru/halcraes/revolut/db/liquibase.xml", new ClassLoaderResourceAccessor(), database);
                liquibase.update("main");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.close();
            }
        } catch (SQLException | LiquibaseException e) {
            throw new InternalException(e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        try (var conn = dataSource.getConnection()) {
            conn.prepareCall("shutdown").execute();
        } catch (Exception e) {
            throw new InternalException(e);
        }
    }
}
