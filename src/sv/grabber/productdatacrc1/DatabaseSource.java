package sv.grabber.productdatacrc1;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Implementation of all necessary methods to interact with SQLite database.
 */
public class DatabaseSource {

    private static final String DB_NAME = "crc.db";
    static final String CONNECTION_STRING = "jdbc:sqlite:db\\" + DB_NAME;

    // "name", "date", "save", "price", "inventory", "available", "keysize", "width", "size", "msrp", "color", "skuid"
    //  0       1       2       3        4              5           6           7       8       9       10      11
    static final DatabaseTable PRODUCTS_TABLE = new DatabaseTable("products",
            new String[] {"name", "keysize", "width", "size", "msrp", "color", "skuid"});           // 0 6 7 8 9 10 11
    static final DatabaseTable PRODUCT_STATS_TABLE = new DatabaseTable("product_data",
            new String[] {"date", "discount", "price", "inventory", "available", "skuid"});             // 1 2 3 4 5 11

    private static final String QUERY_DROP = "DROP TABLE IF EXISTS ";

    private static final String COLUMN_TXT = "TEXT";

    private Connection dbConnection;
    private PreparedStatement updateQuery;
    private ResultSet queryResults;

    void dropTable(Connection dbConnection, DatabaseTable table) {
        executeUpdateQuery(dbConnection, QUERY_DROP + table.getName());
    }
    void createNewTable(Connection dbConnection, DatabaseTable table) {
        executeUpdateQuery(dbConnection, buildCreateTableQuery(table.getColumnNames(), COLUMN_TXT, table.getName()));
    }
    void insertNewData(Connection dbConnection, DatabaseTable table, String[] productData) {
        if (productData.length != table.getColumnNames().length) {        // table fit check
            System.out.println("ERROR Expected " + table.getColumnNames().length + "data items but provided " + productData.length);
            return;
        }
        executeUpdateQueryPrep(dbConnection, buildInsertNewProductPrepQuery(table, productData.length), productData);
    }

    boolean isProductPresent(Connection dbConnection, String productSkuId) {
        // SELECT COUNT(*) FROM <tableName> WHERE skuid = <value>
        List<StringBuilder> queryOutputList = executeQueryPrep(dbConnection,
                "SELECT COUNT(*) FROM " + PRODUCTS_TABLE.getName() + " WHERE skuid = ?", new String[]{productSkuId}, "");
        return isNotZero(queryOutputList);
    }

    boolean isProductDataPresent(Connection dbConnection, String productSkuId, String date) {
        // SELECT COUNT(*) FROM <tableName> WHERE skuid = ? AND date = ?
        List<StringBuilder> queryOutputList = executeQueryPrep(dbConnection,
                "SELECT COUNT(*) FROM " + PRODUCT_STATS_TABLE.getName() + " WHERE skuid = ? AND date = ?", new String[]{productSkuId, date}, "");
        return isNotZero(queryOutputList);
    }

    private boolean isNotZero(List<StringBuilder> queryOutputList) {
        String queryOutput = queryOutputList != null ? queryOutputList.toString() : "";
        String expectedZero = "[0]";
        String expectedOne = "[1]";
        if (queryOutput.equals(expectedZero)) {
            return false;
        } else if (!queryOutput.equals(expectedOne)) {
            System.out.println("WARN Returned FAKE TRUE as expected value " + expectedZero + " or " + expectedOne + " and not " + queryOutput);
        }
        return true;
    }

    void printDataOfParticularProduct(String productSkuid, String separator) {
        System.out.println("_____________________________");
        System.out.println(getProductInfoBySkuid(dbConnection, PRODUCTS_TABLE.getName(), productSkuid, separator));
        System.out.println(getProductInfoBySkuid(dbConnection, PRODUCT_STATS_TABLE.getName(), productSkuid, separator));
    }

    void printDataOfNamedAlikeProducts(String productParValue, String separator) {
        System.out.println("________ [" + productParValue + "] products data from the DB ________");
        String columnName = "name";
        for (StringBuilder name : getColumnDataList(dbConnection, columnName, PRODUCTS_TABLE.getName())) {
            if (name.toString().contains(productParValue)) {
                printDataOfParticularProduct(getProductSkuidByName(dbConnection, columnName, name.toString(), separator), separator);
            }
        }
    }

    void printAllProductData(String title, String separator) {
        System.out.println("________ " + title + " product data from the DB ________");
        for (StringBuilder skuid : getColumnDataList(dbConnection, "skuid", PRODUCTS_TABLE.getName())) {
            printDataOfParticularProduct(skuid.toString(), separator);
        }
    }

    void printChangedProductData(String title, BiFunction<List<StringBuilder>, String, StringBuilder> returnTwoLines, String separator) {
        System.out.println("________ " + title + " product data from the DB ________");
        for (StringBuilder skuid : getColumnDataList(dbConnection, "skuid", PRODUCTS_TABLE.getName())) {
            String foundRecords = getChangedProductInfoForSkuid(dbConnection, PRODUCT_STATS_TABLE.getName(), skuid.toString(), separator, returnTwoLines);
            if (!foundRecords.equals("")) {
                System.out.println("_____________________________");
                System.out.println(getProductInfoBySkuid(dbConnection, PRODUCTS_TABLE.getName(), skuid.toString(), separator));
                System.out.println(foundRecords);
            }
        }
    }

    List<StringBuilder> getColumnDataList(Connection dbConnection, String columnName, String tableName) {
//         SELECT <columnName> FROM <tableName>
        List<StringBuilder> skuidList = executeQuery(dbConnection, "SELECT " + columnName + " FROM " + tableName);
        if (isListNullOrEmpty(skuidList)) {
            System.out.println("No product in the DB yet.");
        }
        return skuidList;
    }

    String getProductInfoBySkuid(Connection dbConnection, String tableName, String parValue, String separator) {
        return getProductInfoByValue(dbConnection, tableName, "skuid", parValue, separator);
    }

    String getProductSkuidByName(Connection dbConnection, String columnName, String parValue, String separator) {
        String productDetails = getProductInfoByValue(dbConnection, PRODUCTS_TABLE.getName(), columnName, parValue, separator);
        return productDetails.substring(productDetails.lastIndexOf(separator) + 1);
    }

    private String getProductInfoByValue(Connection dbConnection, String tableName, String columnName, String value, String separator) {
        // SELECT * FROM <tableName> WHERE <columnName> = ?
        List<StringBuilder> dataList = executeQueryPrep(dbConnection,
                "SELECT * FROM " + tableName + " WHERE " + columnName + " = ?", new String[]{value}, separator);
        StringBuilder formattedData = new StringBuilder();
        if (!isListNullOrEmpty(dataList)) {
            for (StringBuilder dataLine : dataList) {
                formattedData.append(dataLine);
                boolean isLastDataLine = !dataLine.toString().equals(dataList.get(dataList.size() - 1).toString());
                if (isLastDataLine) {
                    formattedData.append("\n\r");
                }
            }
        } else {
            formattedData.append("No data for ").append(value).append(" in ").append(tableName).append(" of the DB yet.");
        }
        return formattedData.toString();
    }

    String getChangedProductInfoForSkuid(Connection dbConnection, String tableName, String skuid, String separator, BiFunction<List<StringBuilder>, String, StringBuilder> returnTwoLines) {
        // SELECT * FROM <tableName> WHERE skuid = ?
        List<StringBuilder> dataList = executeQueryPrep(dbConnection,
                "SELECT * FROM " + tableName + " WHERE skuid = ?", new String[]{skuid}, separator);
        StringBuilder formattedData = new StringBuilder();
        if (!isListNullOrEmpty(dataList)) {
            if (dataList.size() > 1) {
                formattedData = returnTwoLines.apply(dataList, separator);
            }
        } else {
            formattedData.append("No data for ").append(skuid).append(" in ").append(tableName).append(" of the DB yet.");
        }
        return formattedData.toString();
    }

    StringBuilder getLastTwoDifferentLinesIfExist(List<StringBuilder> dataList, String separator) {
        StringBuilder lastTwoLinesBuilder = new StringBuilder();
        StringBuilder lastLineBuilder = dataList.get(dataList.size() - 1);
        String lastLine = getStringWithoutFirstItem(lastLineBuilder, separator);
        StringBuilder prevDifferentLineBuilder = new StringBuilder();
        for (int i = dataList.size() - 2; i > -1; i--) {
            StringBuilder currLineBuilder = dataList.get(i);
            String currLine = getStringWithoutFirstItem(currLineBuilder, separator);
            if (!currLine.equals(lastLine)) {
                prevDifferentLineBuilder = currLineBuilder;
                break;
            }
        }
        if (!prevDifferentLineBuilder.toString().equals("")) {
            lastTwoLinesBuilder.append(prevDifferentLineBuilder).append("\n\r").append(lastLineBuilder);
        }
        return lastTwoLinesBuilder;
    }

    private boolean isListNullOrEmpty(List<StringBuilder> list) {
        return (list == null) || Arrays.toString(list.toArray()).equals("[]") || list.get(0).toString().equals("");
    }

    StringBuilder getLastTwoLinesIfDifferent(List<StringBuilder> dataList, String separator) {
        StringBuilder lastTwoLinesBuilder = new StringBuilder();
        StringBuilder lastLineBuilder = dataList.get(dataList.size() - 1);
        String lastLine = getStringWithoutFirstItem(lastLineBuilder, separator);
        StringBuilder prevLineBuilder = dataList.get(dataList.size() - 2);
        String prevLine = getStringWithoutFirstItem(prevLineBuilder, separator);
        if (!prevLine.equals(lastLine)) {
            lastTwoLinesBuilder.append(prevLineBuilder).append("\n\r").append(lastLineBuilder);
        }
        return lastTwoLinesBuilder;
    }

    private String getStringWithoutFirstItem (StringBuilder strBuilder, String separator) {
        return strBuilder.substring(strBuilder.indexOf(separator) + 1);
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
        try {       // NOTE 2 statements below in separate try-catch blocks cause "[SQLITE_BUSY]  The database file is locked (database is locked)"
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

    Connection open(String connectionString) {
        try {
            dbConnection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            System.out.println("ERROR connecting to " + connectionString.substring(connectionString.lastIndexOf("\\") + 1) + " DB - " + e);
            e.printStackTrace();
        }
        return dbConnection;
    }
    void close() {
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
