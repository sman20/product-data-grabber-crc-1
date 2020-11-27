package sv.grabber.productdatacrc1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

import static sv.grabber.productdatacrc1.PageDataGrabber.SizeType.*;

/**
 * The Page Data Grabber retrieves actual data (size, price, discount, availability, color, etc) for particular set of product variations
 * from CRC product pages and outputs the results to different targets including SQLite database (see {@link PageDataGrabber}).
 <br>Core public access point to the functionality.<br>
 The parser has got an interactive command-line menu through which it is possible to :
 <ul>
 <li>parse list of CRC product pages for product variations with the required sizes and retrieve
 certain product variation with parameters and their values</li>
 <li>show results on the screen</li>
 <li>output results as csv files</li>
 <li>store results in the DB</li>
 <li>read/write configuration data from/to the text <code>[data/crc_*.cfg]</code> and binary <code>[data/crc_*.dat]</code> files</li>
 <li>query the database and prints out all collected data for all the products</li>
 </ul>
 <h3>Configuration files</h3>
 <b>Products data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_links.*]</code> files each line of which contains :
 <ul><li>product names to search for</li><li>their key size name to look for</li><li>URL to the product page</li></ul>
 <i>Format:</i> <code>product_name, key_size_type, url_link_BE_EUR_ENG</code>.
 <br><i>Example:</i> <code>Tire Schwalbe Big Ben, WheelSize, https://www.chainreactioncycles.com/be/en/schwalbe-big-ben-plus-mtb-tyre-greenguard/rp-prod154936</code>
 <p>
 <b>Product sizes to search for data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_sizes.*]</code> files that contain
 set of comma separated sizes in double quotes, each type on a separate line.
 <br><i>Format:</i> <code>"size1", "size2", ...</code>
 <br><i>Example:</i> [<code>"skuId"</code>], [<code>"EU 41", "US 8", "UK 7.5"</code>], [<code>"M", "M/L"</code>], [<code>"26\""</code>].
 </p>
 <p>
 <b>Product parameters to retrieve data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_pars.*]</code> files that contain
 sets of parameter names to retrieve, in the same order as the order of size types in <code>[data/crc_sizes.*]</code>.
 <br><i>Format:</i> <code>"discount", "price", "inventory", "available", "keypar", "width", "size", msrp", "color", "skuid"</code>
 <br><i>Example:</i> <code>"SAVE":, "RP":, "inventoryStatus":, "isInStock":, "WheelSize":, "Width":, "Size":, "RRP":, Colour":, "skuId":</code>.
 </p>
 <p>
 <b>Product common parameters to retrieve data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_search.*]</code> files that contain
 CSS or jQuery-like selectors to look for common product parameters on the pages.
 <br><i>Format:</i> <code>par_name, par_node, par_child_node</code>
 <br><i>Example:</i> <code>SIZES, ul.crcPDPList, li:contains(Size)</code>.
 </p>
 @author S.V.
 @version 0.2.1 #version ${version}
 @since ${DATE}
 */
public class PageDataGrabber {
    private String version = "1.0.0";
    private Document pageNodes;
    private boolean debugMode = false;

    /**
     * Yields current version of the code.
     * @return current version
     */
    public String getVersion() {
        return version;
    }

    /**
     *      Reads initial configuration (products, their sizes to search for, their parameters to retrieve
     *      and common parameters of products) from binary files <code>[data/crc_*.dat]</code>,
     *      if no DAT files, reads initial data from text files <code>[data/crc_*.cfg]</code>
     *      and creates needed tables in the DB if do not exist yet.
     */
    public PageDataGrabber() {
        initializeData();
        initializeDb();
    }

    private OutputType outputType = OutputType.BRIEF;

    private ArrayList<String[]> links;
    private ArrayList<String[]> sizes;
    private ArrayList<String[]> pars;
    private ArrayList<String[]> items;

    private enum OutputType {
        CSV(","),
        DB(";"),
        BRIEF("|"),
        DETAILED(", ");

        private final String separator;

        OutputType(String separator) {
            this.separator = separator;
        }
    }

    public enum SizeType {
        SIZE,
        SHOESSIZE,
        CLOTHINGSIZE,
        WHEELSIZE,
        LENGTH,
        DIAMETER;
    }

     private void initializeData() {
        System.out.println("Initializing data ...");
        links = GrabberFileManipulator.getLinksFromFile(true);
        if (links == null) {
            System.out.println("Downloading links data from CFG files...");
            links = GrabberFileManipulator.getLinksFromFile(false);
        }
        sizes = GrabberFileManipulator.getSizesFromFile(true);
        if (sizes == null) {
            System.out.println("Downloading sizes data from CFG files...");
            sizes = GrabberFileManipulator.getSizesFromFile(false);
        }
        pars = GrabberFileManipulator.getParsFromFile(true);
        if (pars == null) {
            System.out.println("Downloading pars data from CFG files...");
            pars = GrabberFileManipulator.getParsFromFile(false);
        }
        items = GrabberFileManipulator.getPageItemsFromFile(true);
        if (items == null) {
            System.out.println("Downloading pars data from CFG files...");
            items = GrabberFileManipulator.getPageItemsFromFile(false);
        }
    }
    private void initializeDb() {
        DatabaseSource dataSource = new DatabaseSource();
        Connection dbConnection = dataSource.open(DatabaseSource.CONNECTION_STRING);
        System.out.println("Preparing DB...");
        dataSource.createNewTable(dbConnection, DatabaseSource.PRODUCTS_TABLE);
        dataSource.createNewTable(dbConnection, DatabaseSource.PRODUCT_STATS_TABLE);
        dataSource.close();
    }

    private ArrayList<ProductDataset> createProductDatasetsCrc(ArrayList<String[]> linksData, ArrayList<String[]> sizesData,
                                                               ArrayList<String[]> parsData) {
        ArrayList<ProductDataset> productDatasets = new ArrayList<>();

        for (String[] productData : linksData) {
           SizeType sizeType = SizeType.valueOf(productData[1].toUpperCase());
            switch (sizeType) {
                case SIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SIZE.ordinal()), parsData.get(SIZE.ordinal()) ));
                    break;
                case SHOESSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SizeType.SHOESSIZE.ordinal()), parsData.get(SHOESSIZE.ordinal()) ));
                    break;
                case CLOTHINGSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SizeType.CLOTHINGSIZE.ordinal()), parsData.get(CLOTHINGSIZE.ordinal()) ));
                    break;
                case WHEELSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SizeType.WHEELSIZE.ordinal()), parsData.get(WHEELSIZE.ordinal()) ));
                    break;
                case LENGTH:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SizeType.LENGTH.ordinal()), parsData.get(LENGTH.ordinal()) ));
                    break;
                case DIAMETER:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                                sizesData.get(SizeType.DIAMETER.ordinal()), parsData.get(DIAMETER.ordinal()) ));
                    break;
                default:
                    System.out.println("ERROR Unknown size type : " + productData[1]);
            }
        }
        return productDatasets;
    }

    private void parsePages () {
        ArrayList<ProductDataset> pages = createProductDatasetsCrc(links, sizes, pars);
        for (ProductDataset pg : pages) {
            if (debugMode) System.out.println(pg);
            try {
                System.out.println("PRODUCT NAME : " + pg.getName());
                System.out.println("product sizes: " + Arrays.toString(pg.getSizes()));
                pageNodes = Jsoup.connect(pg.getLink()).timeout(5000).get();    // Execute the request as a GET, and parse the result
                if (outputType.equals(OutputType.DETAILED)) {
                    for (String[] item : items) {
                        printProductDataFromPage(item, pageNodes);
                    }
                }
                // PRODUCT VARIANTS BLOCK
                String itemQtyNode = "div.crcPDPVariants";
                Element itemQtyElem = pageNodes.selectFirst(itemQtyNode);
                if (itemQtyElem != null) {
                    Element itemQtyElem2 = itemQtyElem.selectFirst("script:containsData(" + GrabberFileManipulator.getItemVariantsScript() + ")");
                    if (itemQtyElem2 != null) {
                        String[] scriptBlocks = itemQtyElem2.toString().split("}");
                        List<StringBuilder> neededItems = new ArrayList<>();
                        int count = 1;
                        for (String block : scriptBlocks) {                             // blocks of data in script
                            for (String size : pg.getSizes()) {
                                if (block.contains(size) && block.contains(pg.getParNames()[8])) {   // has skuId par; to CHK number if pars.txt chgd
                                    String[] blockLines = block.split("\n");
                                    switch (outputType) {
                                        case CSV:
                                            neededItems.add(collectDataCsv(blockLines, pg));
                                            break;
                                        case DB:
                                            neededItems.add(collectDataDb(blockLines, pg));
                                            break;
                                        case BRIEF:
                                            neededItems.add(collectDataBrief(blockLines, pg));
                                            break;
                                        case DETAILED:
                                            neededItems.add(collectDataDetailed(blockLines, pg, count++));
                                            break;
                                    }
                                }
                            }
                        }
                        switch (outputType) {
                            case CSV:
                                outputResultsCsv(neededItems, pg.getName());
                                break;
                            case DB:
                                outputResultsDb(neededItems);
                                break;
                            case BRIEF:
                                outputResultsBrief(neededItems);
                                break;
                            case DETAILED:
                                outputResultsDetailed(neededItems);
                                break;
                        }
                        if (debugMode) System.out.println("................scriptElements[1] :\n" + scriptBlocks[1]);
                    } else {
                        System.out.println("=========== No " + GrabberFileManipulator.getItemVariantsScript() + " script (script:containsData) found !");
                    }
                } else {
                    System.out.println("========= No PRODUCT VARIANTS (" + itemQtyNode + ") found !\n");
                }
            } catch (IOException e) {
                System.out.println("Failed to connect to the page [" + pg.getLink() + "] : " + e);
            } finally {
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            }
        }
    }

    private void printDbContent() {
        DatabaseSource dataSource = new DatabaseSource();
        dataSource.open(DatabaseSource.CONNECTION_STRING);
        dataSource.printAllProductData("|");
        dataSource.close();
    }

    private StringBuilder collectDataCsv(String[] blockLines, ProductDataset pageData) {
        return collectData(blockLines, pageData, OutputType.CSV.separator);
    }

    private StringBuilder collectDataDb(String[] blockLines, ProductDataset pageData) {
        return collectData(blockLines, pageData, OutputType.DB.separator);
    }

    private StringBuilder collectData(String[] blockLines, ProductDataset pageData, String separator) {
        StringBuilder item = new StringBuilder();
        item.append(pageData.getName()).append(separator)
                .append(getCurrentDate()).append(separator);
        // NOTE, below is LESS efficient but order of results is the SAME as in the CFG files
        for (String par : pageData.getParNames()) {           // not efficient but order of output is controlled by order in the data file
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {      // more efficient if "matches" chk before getParNames loop
                    item.append(line.substring(line.indexOf(":") + 1)
                            .replace("\\\"", "''")
                            .replace("\"", "")
                            .replace(",", "").trim());
                    break;
                }
            }
            item.append(separator);
        }
        return item;
    }

    private StringBuilder collectDataBrief(String[] blockLines, ProductDataset pageData) {
        StringBuilder item = new StringBuilder();
        item.append(getCurrentDate()).append(OutputType.BRIEF.separator);
       for (String par : pageData.getParNames()) {           // less efficient but order of output is controlled by order in the data file
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {      // more efficient if "matches" chk before getParNames loop
                    item.append(line.trim()
                            .replace("\\\"", "''")
                            .replace("\"", "")
                            .replace(",", OutputType.BRIEF.separator));
                    break;
                }
            }
        }
        return item;
    }

    private StringBuilder collectDataDetailed(String[] blockLines, ProductDataset pageData, int count) {
        StringBuilder item = new StringBuilder();
        item.append("Item ").append(count).append("\n");
        for (String par : pageData.getParNames()) {           // less efficient but order of output is controlled by order in the data file
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {      // more efficient if "matches" chk before getParNames loop
                    item.append(line.trim());
                    break;
                }
            }
        }
        return item;
    }

    private void outputResultsCsv(List<StringBuilder> items, String productName) {
        StringBuffer results = new StringBuffer();
        for (int i = 0; i < items.size(); i++) {
            results.append(items.get(i).toString()).append("\r\n");
        }
        String outputFileName = GrabberFileManipulator.storeDataCsvFile(productName.replace(" ", ""), results);
        if (debugMode) System.out.println(results);
        System.out.println("Results are stored in the file: " + outputFileName);
    }

    private void outputResultsDb(List<StringBuilder> items) {
        DatabaseSource dataSource = new DatabaseSource();
        Connection dbConnection = dataSource.open(DatabaseSource.CONNECTION_STRING);
        System.out.println("Inserting product data listed just below...");
        for (int i = 0; i < items.size(); i++) {
            // "name", "date", "save", "price", "inventory", "available", "keysize", "width", "size", "msrp", "color", "skuid"
            //  0       1       2       3        4              5           6           7       8       9       10      11
//            "products", new String[] {"name", "keysize", "width", "size", "msrp", "color", "skuid"}       // 0 6 7 8 9 10 11
//            "product_data", new String[] {"date", "save", "price", "inventory", "available", "skuid"}     // 1 2 3 4 5 11
            if (!dataSource.isProductPresent(dbConnection, items.get(i).toString().split(OutputType.DB.separator)[11])) {      // skuid
                dataSource.insertNewData(dbConnection, DatabaseSource.PRODUCTS_TABLE, new String[]{
                        items.get(i).toString().split(OutputType.DB.separator)[0],
                        items.get(i).toString().split(OutputType.DB.separator)[6],
                        items.get(i).toString().split(OutputType.DB.separator)[7],
                        items.get(i).toString().split(OutputType.DB.separator)[8],
                        items.get(i).toString().split(OutputType.DB.separator)[9],
                        items.get(i).toString().split(OutputType.DB.separator)[10],
                        items.get(i).toString().split(OutputType.DB.separator)[11]
                });
            }
            if (!dataSource.isProductDataPresent(dbConnection, items.get(i).toString().split(OutputType.DB.separator)[11],    // skuid
                                                items.get(i).toString().split(OutputType.DB.separator)[1])) {   // date
                dataSource.insertNewData(dbConnection, DatabaseSource.PRODUCT_STATS_TABLE, new String[]{
                        items.get(i).toString().split(OutputType.DB.separator)[1],
                        items.get(i).toString().split(OutputType.DB.separator)[2],
                        items.get(i).toString().split(OutputType.DB.separator)[3],
                        items.get(i).toString().split(OutputType.DB.separator)[4],
                        items.get(i).toString().split(OutputType.DB.separator)[5],
                        items.get(i).toString().split(OutputType.DB.separator)[11]
                });
            }
            System.out.println(items.get(i));
        }
        dataSource.close();
        System.out.println("Results (if any) are stored in the DB.");
    }

    private void outputResultsBrief(List<StringBuilder> items) {
        for (int i = 0; i < items.size(); i++) {
            System.out.println(items.get(i));
        }
    }

    private void outputResultsDetailed(List<StringBuilder> items) {
        System.out.println("____________neededItems____________");
        for (int i = 0; i < items.size(); i++) {
            System.out.println(items.get(i));
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date dateNow = new Date();
        return dateFormat.format(dateNow);
    }

    private void printProductDataFromPage(String[] dataItem, Document pageDoc) {
        Element itemDescElem = pageDoc.selectFirst(dataItem[1]);        // elements with attribute ID of "id"
        if (itemDescElem != null) {
            if (debugMode) System.out.println(dataItem[1] + "................\n" + itemDescElem.toString());
            Element productValue = itemDescElem.selectFirst(dataItem[2]);
            if (productValue == null) productValue = itemDescElem.selectFirst("li:eq(0)");      // if no searched value (here, Size), get 1st element
            if (debugMode) System.out.println("................" + dataItem[2] + " elem:\n" + productValue.text() + "\n");
            System.out.println("____________ " + dataItem[0] + " : " + productValue.text());
        } else {
            System.out.println("========= No " + dataItem[0] + " (" + dataItem[1] + ") found !");
        }
    }

    private boolean isNull(String name, Object object) {
        if (object == null) {
            System.out.println("No data for " + name);
            return true;
        } else return false;
    }
    private boolean isDataPresent() {
        return (!isNull("Products", links) && !isNull("Sizes", sizes)
                && !isNull("Parameters", pars) && !isNull("Common parameters", items));
    }

    /**
     * Prints currently used configuration (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products).
     */
    public void printCurrentData() {
        System.out.print("products:\n[");
        for (String[] line : links) System.out.print(Arrays.toString(line));
        System.out.print("]\nsizes:\n[");
        for (String[] line : sizes) System.out.print(Arrays.toString(line));
        System.out.print("]\npars:\n[");
        for (String[] line : pars) System.out.print(Arrays.toString(line));
        System.out.print("]\nitems:\n[");
        for (String[] line : items) System.out.print(Arrays.toString(line));
        System.out.println("]");
    }

    /**
     * Retrieves actual product data from pages and prints in brief format.
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link PageDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link PageDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link PageDataGrabber}) of product variations with values.
     */
    public void parsePagesAndShowBrief() {
        outputType = OutputType.BRIEF;
        if (!isDataPresent()) return;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and prints in detailed format.
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link PageDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link PageDataGrabber}),
     * retrieves common product parameters (see <b>Product common parameters to retrieve data</b> in {@link PageDataGrabber}) with values
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link PageDataGrabber}) of product variations with values.
     */
    public void parsePagesAndShowDetailed() {
        outputType = OutputType.DETAILED;
        if (!isDataPresent()) return;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and outputs data of each page in a separated CSV file.
     * <br><code>output/crc_(product_name).(YYYYMMDD)-(HHmmSS).(nn).csv</code>
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link PageDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link PageDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link PageDataGrabber}) of product variations with values.
     * <br><i>Format:</i> <code>product_name,date,discount,price,inventory_status,is_available,key_size,width,size,msrp,color,sku_id,</code>
     * <br><i>Example:</i> <code>Tire Schwalbe Big Ben Plus MTB - GreenGuard,20201008,26%,€24.99,IN STOCK(5+),true,26'',2.15'',Wire Bead,€33.99,Black,sku565936,</code>
     */
    public void parsePagesAndToCsv() {
        outputType = OutputType.CSV;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and stores them in the DB.
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link PageDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link PageDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link PageDataGrabber}) of product variations with values.
     * <br>Then stores static values ("name", "msrp", "keypar", ""width", "size", "color", "skuid") in one DB table
     * and variable ones ("date", "discount", "price", "inventory", "available", "skuid") - in another DB table.
     * <br>The value of SKUID is unique for a product variation and stored in both tables for reconciliation.
     * If a product is already present (by SKUID) in the static data table, its data is not added or updated.
     * If a product variable data is already present (by SKUID and date) in the variable data table,
     * its data is not added.
     */
    public void parsePagesAndToDb() {
        outputType = OutputType.DB;
        parsePages();
    }

    /**
     * Prints out all collected product data from the DB.
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201007|26%|€24.99|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201019|20%|€27.49|IN STOCK(5+)|true|sku565936</code>
     */
    public void printCollectedDbData() {
        outputType = OutputType.DB;
        printDbContent();
    }

    /**
     * Reads configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) from CFG files <code>[data/crc_*.cfg]</code>.
     */
    public void getConfigurationFromCfgFiles() {
//  download products, sizes, search data, parameters from cfg files
        // NOTE does not update the lists from files when changing them on the fly...
        links = GrabberFileManipulator.getLinksFromFile(false);
        sizes = GrabberFileManipulator.getSizesFromFile(false);
        pars = GrabberFileManipulator.getParsFromFile(false);
        items = GrabberFileManipulator.getPageItemsFromFile(false);
        System.out.println("Downloaded configuration from CFG files.");
    }

    /**
     * Writes currently used configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) to CFG files <code>[data/crc_*.cfg]</code>.
     */
    public void putConfigurationToCfgFiles() {
//  upload products, sizes, search data, parameters to cfg files
        GrabberFileManipulator.putLinksToFile(links, false);
        GrabberFileManipulator.putSizesToFile(sizes, false);
        GrabberFileManipulator.putParsToFile(pars, false);
        GrabberFileManipulator.putPageItemsToFile(items, false);
        System.out.println("Stored configuration to CFG files.");
    }

    /**
     * Reads configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) from DAT files <code>[data/crc_*.dat]</code>.
     */
    public void getConfigurationFromDatFiles() {
//  download products, sizes, search data, parameters from dat files
        links = GrabberFileManipulator.getLinksFromFile(true);
        sizes = GrabberFileManipulator.getSizesFromFile(true);
        pars = GrabberFileManipulator.getParsFromFile(true);
        items = GrabberFileManipulator.getPageItemsFromFile(true);
        System.out.println("Downloaded configuration from DAT files.");
    }

    /**
     * Writes currently used configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) to DAT files <code>[data/crc_*.dat]</code>.
     */
    public void putConfigurationToDatFiles() {
//  exit and store current configuration in DAT files, not create a backup (DB)
        GrabberFileManipulator.putLinksToFile(links, true);
        GrabberFileManipulator.putSizesToFile(sizes, true);
        GrabberFileManipulator.putParsToFile(pars, true);
        GrabberFileManipulator.putPageItemsToFile(items, true);
        System.out.println("Stored configuration to DAT files.");
    }
}
