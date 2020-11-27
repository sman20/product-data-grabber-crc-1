package sv.grabber.productdatacrc1;

/**
 * Implementation of database table object to store table and columns names.
 */
public class DatabaseTable {

    private String name;
    private String[] columnNames;

    public DatabaseTable(String name, String[] columnNames) {
        this.name = name;
        this.columnNames = columnNames;
    }

    public String getName() {
        return name;
    }

    public String[] getColumnNames() {
        return columnNames;
    }
}
