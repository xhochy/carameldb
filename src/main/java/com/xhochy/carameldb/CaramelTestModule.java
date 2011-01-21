package com.xhochy.carameldb;

import java.sql.Connection;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Module that injects database related test objects.
 */
class CaramelTestModule extends AbstractModule {

    private Connection conn;
    private String jdbcString;

    /**
     * Create a new module that injects specific database instances.
     * @param conn The connection to be injected.
     * @param jdbcString The connection string according to the injected connection.
     */
    public CaramelTestModule(final Connection conn, final String jdbcString) {
        this.conn = conn;
        this.jdbcString = jdbcString;
    }

    @Override
    protected void configure() {
        bind(Connection.class).annotatedWith(Names.named("testDB")).toInstance(conn);
        bind(String.class).annotatedWith(Names.named("testJDBCString")).toInstance(jdbcString);
    }

}
