package sv.grabber.productdatacrc1;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of all necessary methods to interact with SQLite database.
 */
public class DatabaseSource {

    public static final String DB_NAME = "crc.db";
    public static final String CONNECTION_STRING = "jdbc:sqlite:db\\" + DB_NAME;

    public static final DatabaseTable PRODUCTS_TABLE = new DatabaseTable("products",
            new String[] {"name", "keysize", "width", "size", "msrp", "color", "skuid"});
    public static final DatabaseTable PRODUCT_STATS_TABLE = new DatabaseTable("product_data",
            new String[] {"date", "discount", "price", "inventory", "available", "skuid"});

    public static final String QUERY_DROP = "DROP TABLE IF EXISTS ";

    public static final String COLUMN_TXT = "TEXT";

    private Connection dbConnection;
    private PreparedStatement updateQuery;
    private ResultSet queryResults;

    protected void dropTable(Connection dbConnection, DatabaseTable table) {
        executeUpdateQuery(dbConnection, QUERY_DROP + table.getName());
    }
    protected void createNewTable(Connection dbConnection, DatabaseTable table) {
        executeUpdateQuery(dbConnection, buildCreateTableQuery(table.getColumnNames(), COLUMN_TXT, table.getName()));
    }
    protected void insertNewData(Connection dbConnection, DatabaseTable table, String[] productData) {
        if (productData.length != table.getColumnNames().length) {        // table fit check
            System.out.println("ERROR Expected " + table.getColumnNames().length + "data items but provided " + productData.length);
            return;
        }
        executeUpdateQueryPrep(dbConnection, buildInsertNewProductPrepQuery(table, productData.length), productData);
    }

    protected boolean isProductPresent(Connection dbConnection, String productSkuId) {
        // SELECT COUNT(*) FROM <tableName> WHERE skuid = <value>
        List<StringBuilder> queryOutputList = executeQueryPrep(dbConnection,
                "SELECT COUNT(*) FROM " + PRODUCTS_TABLE.getName() + " WHERE skuid = ?", new String[]{productSkuId}, "");
        assert queryOutputList != null;
        return isNotZero(queryOutputList);
    }

    protected boolean isProductDataPresent(Connection dbConnection, String productSkuId, String date) {
        // SELECT COUNT(*) FROM <tableName> WHERE skuid = ? AND date = ?
        List<StringBuilder> queryOutputList = executeQueryPrep(dbConnection,
                "SELECT COUNT(*) FROM " + PRODUCT_STATS_TABLE.getName() + " WHERE skuid = ? AND date = ?", new String[]{productSkuId, date}, "");
        assert queryOutputList != null;
        return isNotZero(queryOutputList);
    }

    private boolean isNotZero(List<StringBuilder> queryOutputList) {
        String queryOutput = queryOutputList.toString();
        String expectedZero = "[0]";
        String expectedOne = "[1]";
        if (queryOutput.equals(expectedZero)) {
            return false;
        } else if (!queryOutput.equals(expectedOne)) {
            System.out.println("WARN Returned FAKE TRUE as expected value " + expectedZero + " or " + expectedOne + " and not " + queryOutput);
        }
        return true;
    }

    public void printAllProductData(String separator) {
        for (StringBuilder skuid : getColumnDataList(dbConnection, "skuid", PRODUCTS_TABLE.getName())) {
            System.out.println("_____________________________");
            System.out.println(getProductInfoForSkuid(dbConnection, PRODUCTS_TABLE.getName(), skuid.toString(), separator));
            System.out.println(getProductInfoForSkuid(dbConnection, PRODUCT_STATS_TABLE.getName(), skuid.toString(), separator));
        }
    }

    protected List<StringBuilder> getColumnDataList(Connection dbConnection, String columnName, String tableName) {
//         SELECT <columnName> FROM <tableName>
        List<StringBuilder> skuidList = executeQuery(dbConnection, "SELECT " + columnName + " FROM " + tableName);
        if (skuidList == null || skuidList.get(0).toString().equals("")) {
            System.out.println("No product in the DB yet.");
        }
        return skuidList;
    }

    protected String getProductInfoForSkuid(Connection dbConnection, String tableName, String skuid, String separator) {
        // SELECT * FROM <tableName> WHERE skuid = ?
        List<StringBuilder> dataList = executeQueryPrep(dbConnection,
                "SELECT * FROM " + tableName + " WHERE skuid = ?", new String[]{skuid}, separator);
        StringBuilder formattedData = new StringBuilder();
        if (dataList != null && !dataList.get(0).toString().equals("")) {
            for (StringBuilder dataLine : dataList) {
                formattedData.append(dataLine);
                if (!dataLine.toString().equals(dataList.get(dataList.size() - 1).toString())) {
                    formattedData.append("\n\r");
                }
            }
        } else {
            formattedData.append("No data for ").append(skuid).append(" in ").append(tableName).append(" of the DB yet.");
        }
        return formattedData.toString();
    }

    private String buildCreateTableQuery(String[] colNames, String colType, String tableName) {
        // CREATE TABLE IF NOT EXISTS <tableName> (<colName1> <colType>, <colName2> <colType>, <colName3> <colType>)
        StringBuilder sqlQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlQuery.append(tableName).append(" (");
        for (int i = 0; i < colNames.length; i++) {
            sqlQuery.append(colNames[i]).append(" ").append(colType);
            if (i < colNames.length - 1) sqlQuery.append(", ");
        }
        sqlQuery.append(")");
        return sqlQuery.toString();
    }

    private String buildInsertNewProductPrepQuery(DatabaseTable table, int nmbOfValues) {
        // INSERT INTO products (name, stock, save, price) VALUES('shoes', '5 in stock', '57%', '48 eur');
        StringBuilder sqlQuery = new StringBuilder("INSERT INTO ");
        sqlQuery.append(table.getName()).append(" (");
        for (int i = 0; i < table.getColumnNames().length; i++) {
            sqlQuery.append(table.getColumnNames()[i]);
            if (i < table.getColumnNames().length - 1) sqlQuery.append(", ");
        }
        sqlQuery.append(") VALUES(");
        for (int i = 0; i < nmbOfValues; i++) {
            sqlQuery.append("?");
            if (i < nmbOfValues - 1) sqlQuery.append(", ");
        }
        sqlQuery.append(")");
        return sqlQuery.toString();
    }

    private List<StringBuilder> executeQueryPrep(Connection dbConnection, String sqlQuery, String[] sqlPars, String columnSeparator) {
        try {
            if (isPreparedStatementWrong(sqlQuery, sqlPars)) return null;
            finalizePrepStatement(dbConnection, sqlQuery, sqlPars);
            return getQueryResults(updateQuery, columnSeparator);
        } catch (SQLException e) {
            System.out.println("ERROR Executing PreparedStatement [" + sqlQuery + "] - " + e);
            e.printStackTrace();
        }
        return null;
    }

    private List<StringBuilder> executeQuery(Connection dbConnection, String sqlQuery) {
        try {
            updateQuery = dbConnection.prepareStatement(sqlQuery);
            if (sqlQuery.contains("?")) {
                System.out.println("ERROR Expected non-PreparedStatement but provided: " + sqlQuery);
                return null;
            }
            return getQueryResults(updateQuery, "");
        } catch (SQLException e) {
            System.out.println("ERROR Executing [" + sqlQuery + "] - " + e);
            e.printStackTrace();
        }
        return null;
    }

    private void executeUpdateQueryPrep(Connection dbConnection, String sqlQuery, String[] sqlPars) {
        try {
            if (isPreparedStatementWrong(sqlQuery, sqlPars)) return;
            finalizePrepStatement(dbConnection, sqlQuery, sqlPars);
            updateQuery.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ERROR Executing PreparedStatement [" + sqlQuery + "] - " + e);
        }
    }

    private void executeUpdateQuery(Connection dbConnection, String sqlQuery) {
        try {
            if (sqlQuery.contains("?")) {
                System.out.println("ERROR Expected non-PreparedStatement but provided: " + sqlQuery);
                return;
            }
            updateQuery = dbConnection.prepareStatement(sqlQuery);
            updateQuery.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ERROR Executing [" + sqlQuery + "] - " + e);
            e.printStackTrace();
        }
    }

    private void finalizePrepStatement(Connection dbConnection, String query, String[] pars) throws SQLException {
        updateQuery = dbConnection.prepareStatement(query);
        for (int i = 0; i < pars.length; i++) {
            // NOTE single quotes should be removed if present to avoid storing them as part of the value in the DB
            updateQuery.setString(i + 1, pars[i].replace("'", ""));
        }
    }

    private List<StringBuilder> getQueryResults(PreparedStatement prepStatement, String columnSeparator) throws SQLException {
        queryResults = prepStatement.executeQuery();
        List<StringBuilder> results = new ArrayList<>();
        int columns = queryResults.getMetaData().getColumnCount();
        while (queryResults.next()) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < columns; i++) {
                line.append(queryResults.getString(i + 1));
                if (i < columns - 1) line.append(columnSeparator);
            }
            results.add(line);
        }
        return results;
    }

    private boolean isPreparedStatementWrong(String query, String[] pars) {
        int quesMarksCount = query.length() - query.replace("?", "").length();
        if (quesMarksCount != pars.length && !pars[0].equals("")) {
            System.out.println("ERROR Expected " + quesMarksCount + " pars, provided " + pars.length + " pars " + Arrays.toString(pars));
            return true;
        }
        return false;
    }

    protected Connection open(String connectionString) {
        try {
            dbConnection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            System.out.println("ERROR connecting to " + connectionString.substring(connectionString.lastIndexOf("\\") + 1) + " DB - " + e);
            e.printStackTrace();
        }
        return dbConnection;
    }
    protected void close() {
        try {
            if (queryResults != null) queryResults.close();
            if (updateQuery != null) updateQuery.close();
            if (dbConnection != null) dbConnection.close();
        } catch (SQLException e) {
            System.out.println("ERROR closing " + DB_NAME + " connection - " + e);
            e.printStackTrace();
        }
    }
}
