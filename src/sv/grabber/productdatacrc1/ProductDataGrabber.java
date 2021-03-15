package sv.grabber.productdatacrc1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The Product Data Grabber retrieves actual data (size, price, discount, availability, color, etc) for a particular set
 * of product variations from CRC product pages, shows the results and stores them to different targets, like SQLite database, csv files.
 <br>Core public access point to the functionality.<br>
 The grabber has got an interactive command-line menu through which it is possible to :
 <ul>
    <li>parse list of CRC product pages for product variations with the required sizes and retrieve
    certain product variation with parameters and their values</li>
    <li>show results on the screen</li>
    <li>output results to csv files</li>
    <li>store results in the DB</li>
    <li>read/write configuration data from/to the text <code>[data/crc_*.cfg]</code> and binary <code>[data/crc_*.dat]</code> files</li>
    <li>performs exception handling for an incorrect input and missing data</li>
    <li>query the database and print out :</li>
 <li style="list-style-type: none;">
    <ul style="list-style-type: circle;">
        <li>all collected data for all the products</li>
        <li>last changed data for each product</li>
        <li>last changed data for each product that changed since last time</li>
        <li>all collected data for a particular product with given SKU ID</li>
        <li>all collected data for particular products with the name that contains given word(s)</li>
    </ul>
 </li>
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
 @version 1.1.0
 @since 2021-03-14
 */
public class ProductDataGrabber {
    private String version = "1.1.0";
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
     *      and if no DAT files - from text files <code>[data/crc_*.cfg]</code>
     *      and creates needed tables in the DB if do not exist yet.
     */
    public ProductDataGrabber() {
        initializeData();
        initializeDb();
    }

    private OutputType outputType = OutputType.BRIEF;

    private ArrayList<String[]> links;
    private ArrayList<String[]> sizes;
    private ArrayList<String[]> pars;
    private ArrayList<String[]> items;

    void setDebugMode(boolean isDebugMode) {
        this.debugMode = isDebugMode;
    }

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
        DIAMETER
    }

    private void initializeData() {
        System.out.println("Initializing data ...");
        links = GrabberFileManipulator.getLinksFromDatFile();
        if (links == null) {
            System.out.println("No DAT file with links found, downloading data from CFG file...");
            links = GrabberFileManipulator.getLinksFromCfgFile();
        }
        sizes = GrabberFileManipulator.getSizesFromDatFile();
        if (sizes == null) {
            System.out.println("No DAT file with sizes found, downloading data from CFG file...");
            sizes = GrabberFileManipulator.getSizesFromCfgFile();
        }
        pars = GrabberFileManipulator.getParsFromDatFile();
        if (pars == null) {
            System.out.println("No DAT file with parameters found, downloading data from CFG file...");
            pars = GrabberFileManipulator.getParsFromCfgFile();
        }
        items = GrabberFileManipulator.getPageItemsFromDatFile();
        if (items == null) {
            System.out.println("No DAT file with items found, downloading data from CFG file...");
            items = GrabberFileManipulator.getPageItemsFromCfgFile();
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
                            sizesData.get(SizeType.SIZE.ordinal()), parsData.get(SizeType.SIZE.ordinal()) ));
                    break;
                case SHOESSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                            sizesData.get(SizeType.SHOESSIZE.ordinal()), parsData.get(SizeType.SHOESSIZE.ordinal()) ));
                    break;
                case CLOTHINGSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                            sizesData.get(SizeType.CLOTHINGSIZE.ordinal()), parsData.get(SizeType.CLOTHINGSIZE.ordinal()) ));
                    break;
                case WHEELSIZE:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                            sizesData.get(SizeType.WHEELSIZE.ordinal()), parsData.get(SizeType.WHEELSIZE.ordinal()) ));
                    break;
                case LENGTH:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                            sizesData.get(SizeType.LENGTH.ordinal()), parsData.get(SizeType.LENGTH.ordinal()) ));
                    break;
                case DIAMETER:
                    productDatasets.add(new ProductDataset( productData[0], productData[1], productData[2],
                            sizesData.get(SizeType.DIAMETER.ordinal()), parsData.get(SizeType.DIAMETER.ordinal()) ));
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
                pageNodes = Jsoup.connect(pg.getLink()).timeout(10000).get();
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
                        for (String block : scriptBlocks) {
                            for (String size : pg.getSizes()) {
                                boolean containSizeAndSkuid = block.contains(size) && block.contains(pg.getParNames()[8]);
                                if (containSizeAndSkuid) {
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
        DatabaseSource dataSource = getDatabaseSourceInstance();
        dataSource.printAllProductData("ALL", "|");
        dataSource.close();
    }

    private void printParticularProductDbContent(String productSkuid) {
        DatabaseSource dataSource = getDatabaseSourceInstance();
        dataSource.printDataOfParticularProduct(productSkuid, "|");
        dataSource.close();
    }

    private void printNamedAlikeProductsDbContent(String productName) {
        DatabaseSource dataSource = getDatabaseSourceInstance();
        dataSource.printDataOfNamedAlikeProducts(productName, "|");
        dataSource.close();
    }

    private void printChangedDbContent() {
        DatabaseSource dataSource = getDatabaseSourceInstance();
        dataSource.printChangedProductData("CHANGED", dataSource::getLastTwoDifferentLinesIfExist, "|");
        dataSource.close();
    }

    private void printRecentlyChangedDbContent() {
        DatabaseSource dataSource = getDatabaseSourceInstance();
        dataSource.printChangedProductData("RECENTLY changed", dataSource::getLastTwoLinesIfDifferent, "|");
        dataSource.close();
    }

    private DatabaseSource getDatabaseSourceInstance() {
        DatabaseSource dataSource = new DatabaseSource();
        dataSource.open(DatabaseSource.CONNECTION_STRING);
        return dataSource;
    }

    private StringBuilder collectDataCsv(String[] blockLines, ProductDataset pageData) {
        return collectData(blockLines, pageData, OutputType.CSV.separator);
    }

    private StringBuilder collectDataDb(String[] blockLines, ProductDataset pageData) {
        return collectData(blockLines, pageData, OutputType.DB.separator);
    }

    private StringBuilder collectDataBrief(String[] blockLines, ProductDataset pageData) {
        StringBuilder item = new StringBuilder();
        item.append(getCurrentDate()).append(OutputType.BRIEF.separator);
        // NOTE, below is LESS efficient but order of results is the SAME as in the CFG files
        for (String par : pageData.getParNames()) {
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {
                    item.append(buildDataBrief(line));
                    break;
                }
            }
        }
        return item;
    }

    private String buildDataBrief(String line) {
        return line.trim()
                .replace("\\\"", "''")
                .replace("\"", "")
                .replace(",", OutputType.BRIEF.separator);
    }

    private StringBuilder collectData(String[] blockLines, ProductDataset pageData, String separator) {
        StringBuilder item = new StringBuilder();
        item.append(pageData.getName()).append(separator)
                .append(getCurrentDate()).append(separator);
        // NOTE, below is LESS efficient but order of results is the SAME as in the CFG files
        for (String par : pageData.getParNames()) {
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {
                    item.append(buildData(line));
                    break;
                }
            }
            item.append(separator);
        }
        return item;
    }

    private String buildData(String line) {
        return line.substring(line.indexOf(":") + 1)
                .replace("\\\"", "''")
                .replace("\"", "")
                .replace(",", "").trim();
    }

    private StringBuilder collectDataDetailed(String[] blockLines, ProductDataset pageData, int count) {
        StringBuilder item = new StringBuilder();
        item.append("Item ").append(count).append("\n");
        // NOTE, below is LESS efficient but order of results is the SAME as in the CFG files
        for (String par : pageData.getParNames()) {
            for (String line : blockLines) {
                if ((!line.matches("^\\s+\\W?$")) && line.contains(par)) {
                    item.append(buildDataDetailed(line));
                    break;
                }
            }
        }
        return item;
    }

    private String buildDataDetailed(String line) {
        return line.trim();
    }

    private void outputResultsCsv(List<StringBuilder> items, String productName) {
        StringBuffer results = new StringBuffer();
        for (StringBuilder item : items) {
            results.append(item.toString()).append("\r\n");
        }
        String outputFileName = GrabberFileManipulator.storeDataCsvFile(productName.replace(" ", ""), results);
        if (debugMode) System.out.println(results);
        System.out.println("Results are stored in the file: " + outputFileName);
    }

    private void outputResultsDb(List<StringBuilder> items) {
        DatabaseSource dataSource = new DatabaseSource();
        Connection dbConnection = dataSource.open(DatabaseSource.CONNECTION_STRING);
        System.out.println("Inserting product data listed just below...");
        for (StringBuilder item : items) {
//            Database mapping
//            "products", new String[] {"name", "keysize", "width", "size", "msrp", "color", "skuid"}       // 0 6 7 8 9 10 11
//            "product_data", new String[] {"date", "save", "price", "inventory", "available", "skuid"}     // 1 2 3 4 5 11
            // "name", "date", "save", "price", "inventory", "available", "keysize", "width", "size", "msrp", "color", "skuid"
            //  0       1       2       3        4              5           6           7       8       9       10      11
            String skuId = item.toString().split(OutputType.DB.separator)[11];
            if (!dataSource.isProductPresent(dbConnection, skuId)) {
                dataSource.insertNewData(dbConnection, DatabaseSource.PRODUCTS_TABLE, new String[]{
                        item.toString().split(OutputType.DB.separator)[0],
                        item.toString().split(OutputType.DB.separator)[6],
                        item.toString().split(OutputType.DB.separator)[7],
                        item.toString().split(OutputType.DB.separator)[8],
                        item.toString().split(OutputType.DB.separator)[9],
                        item.toString().split(OutputType.DB.separator)[10],
                        skuId
                });
            }
            String dateStamp = item.toString().split(OutputType.DB.separator)[1];
            if (!dataSource.isProductDataPresent(dbConnection, skuId,
                    dateStamp)) {
                dataSource.insertNewData(dbConnection, DatabaseSource.PRODUCT_STATS_TABLE, new String[]{
                        dateStamp,
                        item.toString().split(OutputType.DB.separator)[2],
                        item.toString().split(OutputType.DB.separator)[3],
                        item.toString().split(OutputType.DB.separator)[4],
                        item.toString().split(OutputType.DB.separator)[5],
                        skuId
                });
            }
            System.out.println(item);
        }
        dataSource.close();
        System.out.println("Results (if any) are stored in the DB.");
    }

    private void outputResultsBrief(List<StringBuilder> items) {
        for (StringBuilder item : items) System.out.println(item);
    }

    private void outputResultsDetailed(List<StringBuilder> items) {
        System.out.println("____________neededItems____________");
        outputResultsBrief(items);
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
            if (productValue == null) productValue = itemDescElem.selectFirst("li:eq(0)");
            if (productValue == null) productValue = new Element("DUMMY");
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
    private boolean isDataAbsent() {
        return (isNull("Products", links) || isNull("Sizes", sizes)
                || isNull("Parameters", pars) || isNull("Common parameters", items));
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
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link ProductDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link ProductDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link ProductDataGrabber}) of product variations with values.
     */
    public void parsePagesAndShowBrief() {
        outputType = OutputType.BRIEF;
        if (isDataAbsent()) return;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and prints in detailed format.
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link ProductDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link ProductDataGrabber}),
     * retrieves common product parameters (see <b>Product common parameters to retrieve data</b> in {@link ProductDataGrabber}) with values
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link ProductDataGrabber}) of product variations with values.
     */
    public void parsePagesAndShowDetailed() {
        outputType = OutputType.DETAILED;
        if (isDataAbsent()) return;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and outputs data of each product variation in a separated CSV file.
     * <br><code>output/crc_(product_name).(YYYYMMDD)-(HHmmSS).(nn).csv</code>
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link ProductDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link ProductDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link ProductDataGrabber}) of product variations with values.
     * <br><i>Format:</i> <code>product_name,date,discount,price,inventory_status,is_available,key_size,width,size,msrp,color,sku_id,</code>
     * <br><i>Example:</i> <code>Tire Schwalbe Big Ben Plus MTB - GreenGuard,20201008,26%,&euro;24.99,IN STOCK(5+),true,26'',2.15'',Wire Bead,&euro;33.99,Black,sku565936,</code>
     */
    public void parsePagesAndToCsv() {
        outputType = OutputType.CSV;
        parsePages();
    }

    /**
     * Retrieves actual product data from pages and stores them in the DB.
     * <br>Searches for product variations on the pages (see <b>Products data</b> in {@link ProductDataGrabber})
     * that have particular sizes (see <b>Product sizes to search for data</b> in {@link ProductDataGrabber})
     * and retrieves parameters (see <b>Product parameters to retrieve data</b> in {@link ProductDataGrabber}) of product variations with values.
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
     * Prints out all collected data for all products from the DB.
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201007|26%|&euro;24.99|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201019|20%|&euro;27.49|IN STOCK(5+)|true|sku565936</code>
     */
    public void printAllCollectedDbData() {
        printDbContent();
    }

    /**
     * Prints out from the DB collected product data that has changed any time in the past.
     * I.e. the last data record of a product and the closest data record that was different earlier.
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201010|20%|&euro;27.49|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201019|20%|&euro;27.49|Out Of Stock|false|sku565936</code>
     */
    public void printChangedDbData() {
        printChangedDbContent();
    }

    /**
     * Prints out from the DB collected product data that has just changed,
     * i.e. the last two data records of a product that are different.
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201007|26%|&euro;24.99|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201008|20%|&euro;27.49|IN STOCK(5+)|true|sku565936</code>
     */
    public void printRecentlyChangedDbData() {
        printRecentlyChangedDbContent();
    }

    /**
     * Prints out all collected data for a product (by its unique SKU ID) from the DB.
     * @param productSkuid SKU ID code of the product (like "sku565936").
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201007|26%|&euro;24.99|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201008|20%|&euro;27.49|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201019|20%|&euro;27.49|Out Of Stock|false|sku565936</code>
     */
    public void printParticularProductDbData(String productSkuid) {
        printParticularProductDbContent(productSkuid);
    }

    /**
     * Prints out all collected data for all products from the DB that contain given word(s) in their name.
     * @param productName Name of the product (like "Schwalbe Big Ben Plus").
     * <br><i>Format:</i>
     * <br><code>name|key_size|width|size|msrp|color|skuid</code>
     * <br><code>date1|discount1|price1|inventory1|available1|skuid</code>
     * <br><code>date2|discount2|price2|inventory2|available2|skuid</code>
     * <br><i>Example:</i>
     * <br><code>Tire Schwalbe Big Ben Plus MTB - GreenGuard|26''|2.15''|Wire Bead|&euro;33.99|Black|sku565936</code>
     * <br><code>20201007|26%|&euro;24.99|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201008|20%|&euro;27.49|IN STOCK(5+)|true|sku565936</code>
     * <br><code>20201019|20%|&euro;27.49|Out Of Stock|false|sku565936</code>
     */
    public void printNamedAlikeProductsDbData(String productName) {
        printNamedAlikeProductsDbContent(productName);
    }

    /**
     * Reads configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) from CFG files <code>[data/crc_*.cfg]</code>.
     */
    public void getConfigurationFromCfgFiles() {
//  download products, sizes, search data, parameters from cfg files
        links = GrabberFileManipulator.getLinksFromCfgFile();
        sizes = GrabberFileManipulator.getSizesFromCfgFile();
        pars = GrabberFileManipulator.getParsFromCfgFile();
        items = GrabberFileManipulator.getPageItemsFromCfgFile();
        System.out.println("Configuration downloaded from CFG files.");
    }

    /**
     * Writes currently used configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) to CFG files <code>[data/crc_*.cfg]</code>.
     */
    public void putConfigurationToCfgFiles() {
//  upload products, sizes, search data, parameters to cfg files
        GrabberFileManipulator.putLinksToCfgFile(links);
        GrabberFileManipulator.putSizesToCfgFile(sizes);
        GrabberFileManipulator.putParsToCfgFile(pars);
        GrabberFileManipulator.putPageItemsToCfgFile(items);
        System.out.println("Configuration stored to CFG files.");
    }

    /**
     * Reads configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) from DAT files <code>[data/crc_*.dat]</code>.
     */
    public void getConfigurationFromDatFiles() {
//  download products, sizes, search data, parameters from dat files
        links = GrabberFileManipulator.getLinksFromDatFile();
        sizes = GrabberFileManipulator.getSizesFromDatFile();
        pars = GrabberFileManipulator.getParsFromDatFile();
        items = GrabberFileManipulator.getPageItemsFromDatFile();
        System.out.println("Configuration downloaded from DAT files.");
    }

    /**
     * Writes currently used configuration data (products, their sizes to search for,
     * their parameters to retrieve and common parameters of products) to DAT files <code>[data/crc_*.dat]</code>.
     */
    public void putConfigurationToDatFiles() {
//  exit and store current configuration in DAT files, not create a backup (DB)
        GrabberFileManipulator.putLinksToDatFile(links);
        GrabberFileManipulator.putSizesToDatFile(sizes);
        GrabberFileManipulator.putParsToDatFile(pars);
        GrabberFileManipulator.putPageItemsToDatFile(items);
        System.out.println("Configuration stored to DAT files.");
    }
}
