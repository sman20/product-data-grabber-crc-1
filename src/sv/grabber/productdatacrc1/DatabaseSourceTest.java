package sv.grabber.productdatacrc1;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.*;

@TestMethodOrder(OrderAnnotation.class)
class DatabaseSourceTest {
    public static final String TEST_DB_NAME = "test.db";
    public static final String TEST_CONNECTION_STRING = "jdbc:sqlite:db\\" + TEST_DB_NAME;

    public static final DatabaseTable TEST_PRODUCTS_TABLE = new DatabaseTable("test_products",
            new String[] {"t_name", "t_keysize", "t_width", "t_size", "t_msrp", "t_color", "t_skuid"});           // 0 6 7 8 9 10 11
    public static final DatabaseTable TEST_PRODUCT_STATS_TABLE = new DatabaseTable("test_product_data",     // for the future
            new String[] {"t_date", "t_discount", "t_price", "t_inventory", "t_available", "t_skuid"});             // 1 2 3 4 5 11

    public static final String[][] TEST_PRODUCTS = new String[][]{
                    {"'Product 1 Wrench'", "'One Size'", "''", "'One Size'", "'€11'", "'Black'", "'sku000001'"},
                    {"'Product 2 Shoes'", "'US 7.5'", "''", "''", "'€22'", "'Silver'", "'sku000002'"},
                    {"'Product 3 Shorts'", "'M'", "''", "''", "'€33'", "'Bronze'", "'sku000003'"},
                    {"'Product 4 Tire'", "'26\"'", "'2.10'", "'Folding Bead'", "'€44'", "'Black - Reflective'", "'sku000004'"}};
    public static final String[] TMP_TEST_PRODUCT = new String[]{"'Product TMP Lube'", "'100ml'", "''", "'100ml'", "'€99'", "''", "'sku100000'"};

    public static final String[][] TEST_PRODUCT_1_DATA = new String[][]{
                                        {"'20200101'", "'10%'", "'€10'", "'IN STOCK (11+)'", "'true'", "'sku000001'"},
                                        {"'20200102'", "'10%'", "'€10'", "'Out of stock'", "'false'", "'sku000001'"}};
    public static final String[][] TEST_PRODUCT_2_DATA = new String[][]{
                                        {"'20200101'", "'20%'", "'€20'", "'IN STOCK (22+)'", "'true'", "'sku000002'"},
                                        {"'20200102'", "'20%'", "'€20'", "'Only (2) left'", "'true'", "'sku000002'"}};
    public static final String[][] TEST_PRODUCT_3_DATA = new String[][]{
                                        {"'20200101'", "'30%'", "'€30'", "'IN STOCK (33+)'", "'true'", "'sku000003'"},
                                        {"'20200102'", "'30%'", "'€30'", "'Only (3) left'", "'true'", "'sku000003'"}};
    public static final String[][] TEST_PRODUCT_4_DATA = new String[][]{
                                        {"'20200101'", "'40%'", "'€40'", "'IN STOCK (44+)'", "'true'", "'sku000004'"},
                                        {"'20200102'", "'40%'", "'€40'", "'Only (4) left'", "'true'", "'sku000004'"}};
    public static final String[][] TMP_TEST_PRODUCT_DATA = new String[][]{
                                {"'20201111'", "'99%'", "'€99'", "'IN STOCK (99++)'", "'true'", "'sku000004'"},
                                {"'20200101'", "'99%'", "'€99'", "'IN STOCK (99++)'", "'true'", "'sku400000'"},
                                {"'20201111'", "'99%'", "'€99'", "'IN STOCK (99++)'", "'true'", "'sku400000'"}};
    public static final String[][][] TEST_PRODUCT_DATA_MATRIX = new String[][][]{TEST_PRODUCT_1_DATA,
                                                                                TEST_PRODUCT_2_DATA,
                                                                                TEST_PRODUCT_3_DATA,
                                                                                TEST_PRODUCT_4_DATA};

    private DatabaseSource testDbSource;
    private Connection testDbConnection;

    @BeforeAll
    static void beforeSuit() throws SQLException {
        System.out.println("TEST: Starting " + DatabaseSourceTest.class.getSimpleName() + " test set...");
//        prepareTestDb();              // to prepare and fulfill test DB with test data initially
    }

    @AfterAll
    static void afterSuit() {
        System.out.print("TEST: The " + DatabaseSourceTest.class.getSimpleName() + " test set finished.");
    }

    @BeforeEach
    void setUp() throws SQLException {
        System.out.println("TEST: Starting new test...");
        testDbSource = new DatabaseSource();
        testDbConnection = DriverManager.getConnection(TEST_CONNECTION_STRING);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testDbConnection != null) testDbConnection.close();
        if (testDbSource != null) testDbSource.close();
        System.out.print("TEST: The test finished and DB connection closed.");
    }

//    @Disabled
    @Order(3)
    @Test
    void createAndDropNewTable() throws SQLException {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        try {
            System.out.print("TEST: Checking a to-be-created table does not exist...");
            selectColumnsFromTable(testDbConnection, TEST_PRODUCTS_TABLE);
            fail("TEST: No java.sql.SQLException thrown ! The table should not exist !");
        } catch (SQLException initialEx) {
            assertTrue(initialEx.toString().contains("SQL error or missing database (no such table:"));
            System.out.println("\tPASSED");
            System.out.print("TEST: Checking the new table created...");
            testDbSource.createNewTable(testDbConnection, TEST_PRODUCTS_TABLE);
            List<ResultSet> resultSets = selectColumnsFromTable(testDbConnection, TEST_PRODUCTS_TABLE);
            for (ResultSet set : resultSets) {
                List<StringBuilder> results = getResultSet(set, "|");
                assertEquals("[]", Arrays.toString(results.toArray()));
            }
            System.out.println("\tPASSED");
            testDbSource.dropTable(testDbConnection, TEST_PRODUCTS_TABLE);
            try {
                System.out.print("TEST: Checking the new table dropped...");
                selectColumnsFromTable(testDbConnection, TEST_PRODUCTS_TABLE);
                fail("TEST: No java.sql.SQLException thrown ! The table should not exist !");
            } catch (SQLException droppedEx) {
                assertTrue(droppedEx.toString().contains("SQL error or missing database (no such table:"));
                System.out.println("\tPASSED");
            }
        }
    }

//    @Disabled
    @Order(3)
    @Test
    void insertNewData() throws SQLException {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        int newProductNmb = (int) (Math.random() * TEST_PRODUCTS.length);       // randomly choosing data set
        try {
            testDbConnection.setAutoCommit(false);          // transaction start
            System.out.println("TEST: Counting a product to add qty...");
//            "SELECT COUNT(*) FROM tableName WHERE skuid = ?"
            int countBefore = countAllFromWhereSkuid(testDbConnection, DatabaseSource.PRODUCTS_TABLE, TEST_PRODUCTS[newProductNmb][TEST_PRODUCTS[newProductNmb].length - 1]);
            System.out.println("TEST: Inserting the product [" + newProductNmb + "]...");
            testDbSource.insertNewData(testDbConnection, DatabaseSource.PRODUCTS_TABLE, TEST_PRODUCTS[newProductNmb]);
            int countAfter = countAllFromWhereSkuid(testDbConnection, DatabaseSource.PRODUCTS_TABLE, TEST_PRODUCTS[newProductNmb][TEST_PRODUCTS[newProductNmb].length - 1]);
            System.out.print("TEST: Checking resulting product qty...");
            assertEquals(countBefore + 1, countAfter);
            System.out.println("\tPASSED");
        } catch (NumberFormatException nEx) {
            System.out.println("TEST: ERROR getting int from countAllFromWhere - " + nEx);
            throw nEx;
        } finally {
            testDbConnection.rollback();                // transaction finish
        }
    }

//    @Disabled
//    @Order(3)
    @Test
    void isProductPresent() throws SQLException {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        assertProductAvailability(testDbSource, testDbConnection, new String[][]{TMP_TEST_PRODUCT}, false);
        assertProductAvailability(testDbSource, testDbConnection, TEST_PRODUCTS, true);
    }
    private void assertProductAvailability(DatabaseSource testDbSource, Connection testDbConnection, String[][] productArray, boolean shouldBePresent) throws SQLException {
        String message = shouldBePresent ? "is present is found" : "is absent is not found";
        System.out.print("TEST: Checking that product which " + message + "...");
//            "SELECT COUNT(*) FROM tableName WHERE skuid = ?"
        for (String[] product : productArray) {
            int productQty = countAllFromWhereSkuid(testDbConnection, DatabaseSource.PRODUCTS_TABLE, product[product.length - 1]);
            assertEquals(shouldBePresent ? productQty == 1 : productQty != 0, testDbSource.isProductPresent(testDbConnection, product[product.length - 1]));
        }
        System.out.println("\tPASSED");
    }

//    @Disabled
//    @Order(3)
    @Test
    void isProductDataPresent() throws SQLException {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        assertProductDataAvailability(testDbSource, testDbConnection, TMP_TEST_PRODUCT_DATA, false);
        for (String[][] productDataRecords : TEST_PRODUCT_DATA_MATRIX) {
            assertProductDataAvailability(testDbSource, testDbConnection, productDataRecords, true);
        }
    }
    private void assertProductDataAvailability(DatabaseSource testDbSource, Connection testDbConnection, String[][] productDataArray, boolean shouldBePresent) throws SQLException {
        String message = shouldBePresent ? "is present is found" : "is absent is not found";
        System.out.print("TEST: Checking that product data which " + message + "...");
//            "SELECT COUNT(*) FROM tableName WHERE skuid = ?"
        for (String[] productData : productDataArray) {
            int productDataQty = countAllFromWhereSkuidAndDate(
                    testDbConnection, DatabaseSource.PRODUCT_STATS_TABLE, productData[productData.length - 1], productData[0]);
            assertEquals(shouldBePresent ? productDataQty == 1 : productDataQty != 0,
                    testDbSource.isProductDataPresent(testDbConnection, productData[productData.length - 1], productData[0]));
        }
        System.out.println("\tPASSED");
    }

//    @Disabled
    @Order(3)
    @Test
    void getColumnData() {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        System.out.print("TEST: Checking that product column data retrieved correctly...");
        List<String> prodColDataBuilderExpected = new ArrayList<>();
        for (String[] prodData : TEST_PRODUCTS) {
            prodColDataBuilderExpected.add(prodData[prodData.length - 1].replace("'", ""));
        }
        List<StringBuilder> prodColDataActual = testDbSource.getColumnDataList(testDbConnection, "skuid", DatabaseSource.PRODUCTS_TABLE.getName());
        for (int i = 0; i < prodColDataBuilderExpected.size(); i++) {
            assertEquals(prodColDataBuilderExpected.get(i), prodColDataActual.get(i).toString());
        }
        System.out.println("\tPASSED");
    }

//    @Disabled
    @Order(3)
    @Test
    void getProductInfo() {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        int testProdNmb = (int) (Math.random() * TEST_PRODUCTS.length);
        System.out.print("TEST: Checking that product ["+ testProdNmb + "] info retrieved correctly...");
        String separator = "|";
        StringBuilder prodInfoBuilderExpected = new StringBuilder();
        for (int i = 0; i < TEST_PRODUCTS[testProdNmb].length; i++) {
            prodInfoBuilderExpected.append(TEST_PRODUCTS[testProdNmb][i].replace("'", ""));
            if (i < TEST_PRODUCTS[testProdNmb].length - 1) prodInfoBuilderExpected.append(separator);
        }
        String prodInfoActual = testDbSource.getProductInfoForSkuid(testDbConnection, DatabaseSource.PRODUCTS_TABLE.getName(), TEST_PRODUCTS[testProdNmb][TEST_PRODUCTS[testProdNmb].length - 1], separator);
        assertEquals(prodInfoBuilderExpected.toString(), prodInfoActual);
        System.out.println("\tPASSED");
    }

//    @Disabled
    @Order(3)
    @Test
    void getProductData() {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        int testProdNmb = (int) (Math.random() * TEST_PRODUCTS.length);     // randomly choosing data set
        System.out.print("TEST: Checking that collected product ["+ testProdNmb + "] data retrieved correctly...");
        String separator = "|";
        StringBuilder prodDataBuilderExpected = getCollectedProductData(TEST_PRODUCT_DATA_MATRIX[testProdNmb], separator);
        String prodDataActual = testDbSource.getProductInfoForSkuid(testDbConnection,
                                                                DatabaseSource.PRODUCT_STATS_TABLE.getName(),
                                                                TEST_PRODUCTS[testProdNmb][TEST_PRODUCTS[testProdNmb].length - 1],
                                                                separator);
        assertEquals(prodDataBuilderExpected.toString(), prodDataActual);
        System.out.println("\tPASSED");
    }
    private StringBuilder getCollectedProductData(String[][] prodDataArray, String separator) {
        StringBuilder prodDataBuilderExpected = new StringBuilder();
        for (int i = 0; i < prodDataArray.length; i++) {
            for (int j = 0; j < prodDataArray[i].length; j++) {
                prodDataBuilderExpected.append(prodDataArray[i][j].replace("'", ""));
                if (j < prodDataArray[i].length - 1) prodDataBuilderExpected.append(separator);
            }
            if (i < prodDataArray.length - 1) prodDataBuilderExpected.append("\n\r");
        }
        return prodDataBuilderExpected;
    }

    @Order(3)
    @Test
    void openAndClose() throws SQLException {
        System.out.println("_______  TEST: " + new Object(){}.getClass().getEnclosingMethod().getName() + "  _______");
        DatabaseSource tDbSource = new DatabaseSource();
        Connection tDbConnection = null;
        try {
            tDbConnection = tDbSource.open(TEST_CONNECTION_STRING);
            System.out.print("TEST: Checking connection to the DB...");
            assertNotNull(tDbConnection);
            System.out.println("\tPASSED");
            tDbSource.close();
            System.out.print("TEST: Checking connection to the DB is closed...");
            Statement statement = tDbConnection.createStatement();
            statement.execute("SELECT 1");
            fail("TEST: No java.sql.SQLException thrown !");
        } catch (SQLException e) {
            assertTrue(e.toString().contains("database connection closed"));
            System.out.println("\tPASSED");
        } finally {
            if (tDbConnection != null) tDbConnection.close();
            if (tDbSource != null) tDbSource.close();
        }
    }

    private static String formStringForSql(String[] strings, String type, String separator) {
        StringBuilder concatenatedString = new StringBuilder("(");
        for (String str : strings) {
            concatenatedString.append(str);
            if (!type.equals("")) concatenatedString.append(" ").append(type);
            if (!str.equals(strings[strings.length - 1])) concatenatedString.append(", ");
        }
        concatenatedString.append(")");
        return concatenatedString.toString();
    }

    private List<ResultSet> selectColumnsFromTable(Connection testDbConnection, DatabaseTable table) throws SQLException {
        List<ResultSet> results = new ArrayList<>();
        for (int i = 0; i < table.getColumnNames().length; i++) {
            PreparedStatement prepStatement = testDbConnection.prepareStatement(
                    "SELECT " + table.getColumnNames()[i] + " FROM " + table.getName());
            results.add(prepStatement.executeQuery());
        }
        return results;
    }

    private int countAllFromWhereSkuid(Connection testDbConnection, DatabaseTable table, String skuidValue) throws SQLException {
        //        "SELECT COUNT(*) FROM tableName WHERE skuid = ?"
        PreparedStatement prepStatement = testDbConnection.prepareStatement(
                "SELECT COUNT(*) FROM " + table.getName() + " WHERE skuid = " + skuidValue);
        List<StringBuilder> results = getResultSet(prepStatement.executeQuery(), " ");
        return Integer.parseInt(Arrays.toString(results.toArray()).replace("[", "").replace("]", ""));
    }

    private int countAllFromWhereSkuidAndDate(Connection testDbConnection, DatabaseTable table, String skuidValue, String date) throws SQLException {
        //        "SELECT COUNT(*) FROM tableName WHERE skuid = ? AND date = ?"
        PreparedStatement prepStatement = testDbConnection.prepareStatement(
                "SELECT COUNT(*) FROM " + table.getName() + " WHERE skuid = " + skuidValue + " AND date = " + date);
        List<StringBuilder> results = getResultSet(prepStatement.executeQuery(), " ");
        return Integer.parseInt(Arrays.toString(results.toArray()).replace("[", "").replace("]", ""));
    }

    private List<StringBuilder> getResultSet(ResultSet queryResults, String columnSeparator) throws SQLException {
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

    // to prepare and fulfill test DB with test data initially
    private static void prepareTestDb() throws SQLException {
        System.out.println("TEST: Preparing a test DB...");
        DatabaseSource tDbSource = new DatabaseSource();
        Connection tDbConnection = tDbSource.open(TEST_CONNECTION_STRING);
//            testDbConnection.setAutoCommit(false);
        Statement prepareDbStatement = tDbConnection.createStatement();
        prepareDbStatement.execute("CREATE TABLE IF NOT EXISTS " + DatabaseSource.PRODUCTS_TABLE.getName()
                + formStringForSql(DatabaseSource.PRODUCTS_TABLE.getColumnNames(), "TEXT", ", "));
        prepareDbStatement.execute("CREATE TABLE IF NOT EXISTS " + DatabaseSource.PRODUCT_STATS_TABLE.getName()
                + formStringForSql(DatabaseSource.PRODUCT_STATS_TABLE.getColumnNames(), "TEXT", ", "));

        for (String[] product : TEST_PRODUCTS) {
            prepareDbStatement.execute("INSERT INTO " + DatabaseSource.PRODUCTS_TABLE.getName()
                    + " " + formStringForSql(DatabaseSource.PRODUCTS_TABLE.getColumnNames(), "", ", ")
                    + " VALUES" + formStringForSql(product, "", ", "));
        }

        for (String[][] productDataRecords : TEST_PRODUCT_DATA_MATRIX) {
            for (String[] productData : productDataRecords) {
                prepareDbStatement.execute("INSERT INTO " + DatabaseSource.PRODUCT_STATS_TABLE.getName()
                        + " " + formStringForSql(DatabaseSource.PRODUCT_STATS_TABLE.getColumnNames(), "", ", ")
                        + " VALUES" + formStringForSql(productData, "", ", "));
            }
        }
        tDbConnection.close();
        tDbSource.close();
        System.out.println(" TEST: The test DB prepared.");
    }
}