package sv.grabber.productdatacrc1;

/**
 * Implementation of database table object to store table and columns names.
 */
public class DatabaseTable {

    private String name;
    private String[] columnNames;

    DatabaseTable(String name, String[] columnNames) {
        this.name = name;
        this.columnNames = columnNames;
    }

    String getName() {
        return name;
    }

    String[] getColumnNames() {
        return columnNames;
    }
}
