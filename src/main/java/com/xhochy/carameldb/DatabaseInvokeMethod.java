package com.xhochy.carameldb;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.derby.impl.io.VFMemoryStorageFactory;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.model.FrameworkMethod;
import org.yaml.snakeyaml.Yaml;

import com.google.inject.Guice;

/**
 * Invoker that resets the database before each test.
 */
class DatabaseInvokeMethod extends InvokeMethod {
    private Object target;
    private String databaseFixtures = "/db-fixtures.yml";
    private Connection conn;
    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String JDBC_PREFIX = "jdbc:derby:memory:";

    public DatabaseInvokeMethod(final FrameworkMethod testMethod, final Object target) {
        super(testMethod, target);
        CaramelFixture fixture = testMethod.getAnnotation(CaramelFixture.class);
        if (fixture != null) {
            databaseFixtures = fixture.value();
        }
        this.target = target;
    }

    private void setUpDatabase() throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            SQLException, FileNotFoundException {
        Class.forName(DRIVER).newInstance();
        conn = DriverManager.getConnection(getJDBCString() + "create=true");
        loadFixtures();
    }

    @SuppressWarnings("unchecked")
    private void loadFixtures() throws FileNotFoundException, SQLException {
        // Load the fixtures file
        InputStream io = getClass().getResourceAsStream(databaseFixtures);
        Yaml yaml = new Yaml();

        // Parse the fixtures file
        Map<String, Object> data = (Map<String, Object>) yaml.load(io);
        initTables((Map<String, Map<String, String>>) data.get("tables"));
        initData((Map<String, List<Map<String, Object>>>) data.get("data"));
    }

    private void initData(final Map<String, List<Map<String, Object>>> data) throws SQLException {
        // If there is no data, we do not need to do something.
        if (data == null) {
            return;
        }

        // Insert the data table by table, row by row.
        for (Entry<String, List<Map<String, Object>>> outerEntry : data.entrySet()) {
            for (Map<String, Object> entry : outerEntry.getValue()) {
                // Step 1: Prepare the statement
                String[] keys = entry.keySet().toArray(new String[0]);
                StringBuffer sql = new StringBuffer("INSERT INTO ");
                sql.append(outerEntry.getKey()).append(" (");
                StringBuffer sql2 = new StringBuffer(") VALUES (");
                for (int i = 0; i < keys.length; i++) {
                    if (i != 0) {
                        sql.append(", ");
                        sql2.append(", ");
                    }
                    sql2.append('?');
                    sql.append("\"").append(keys[i]).append("\"");
                }
                sql.append(sql2).append(")");

                // Step 2: Fill it with values.
                PreparedStatement stmt = conn.prepareStatement(sql.toString());
                for (int i = 0; i < keys.length; i++) {
                    if (entry.get(keys[i]) instanceof Integer) {
                        stmt.setInt(i + 1, (Integer) entry.get(keys[i]));
                    } else if (entry.get(keys[i]) instanceof String) {
                        stmt.setString(i + 1, (String) entry.get(keys[i]));
                    } else {
                        throw new IllegalArgumentException("Do not know which data type to use");
                    }
                }

                // Step 3: Execute it.
                stmt.executeUpdate();
            }
        }
    }

    private void initTables(final Map<String, Map<String, String>> data) throws SQLException {
        for (Entry<String, Map<String, String>> outerEntry : data.entrySet()) {
            StringBuffer sql = new StringBuffer("CREATE TABLE ");
            sql.append(outerEntry.getKey()).append(" (");
            for (Entry<String, String> innerEntry : outerEntry.getValue().entrySet()) {
                sql.append("\"").append(innerEntry.getKey()).append("\" ");

                // TODO Make this a separate function and add more types.
                if (innerEntry.getValue().equals("integer")) {
                    sql.append("INT");
                } else if (innerEntry.getValue().equals("varchar")) {
                    sql.append("VARCHAR(255)");
                }
                sql.append(", ");
            }
            // remove last comma
            sql.replace(sql.lastIndexOf(","), sql.length(), "").append(")");
            conn.prepareCall(sql.toString()).execute();
        }
    }

    /**
     * The JDBC connection string to connect to the database.
     * @return JDBCConnection String terminated with ';'
     */
    private String getJDBCString() {
        return JDBC_PREFIX + Integer.toHexString(target.hashCode()) + ";";
    }

    /**
     * Clean up the database sued in the unit tests.
     */
    private void tearDownDatabase() {
        boolean shutdown = false;
        try {
            conn.close();
            DriverManager.getConnection(getJDBCString() + "shutdown=true");
        } catch (SQLException e) {
            // Database shutdowns always trigger an exception to show it completed.
            shutdown = true;
        }
        if (shutdown) {
            VFMemoryStorageFactory.purgeDatabase(Integer.toHexString(target.hashCode()));
        }
    }

    @Override
    public void evaluate() throws Throwable {
        setUpDatabase();
        Guice.createInjector(new CaramelTestModule(conn, getJDBCString())).injectMembers(target);
        super.evaluate();
        tearDownDatabase();
    }

}
