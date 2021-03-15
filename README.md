# product-data-grabber-crc-1
First release of Product Data Grabber Crc1.

 The Product Data Grabber retrieves actual data (size, price, discount, availability, color, etc) for a particular set
 of product variations from CRC product pages, shows the results and stores them to different targets, like SQLite database, csv files.
 <br>Core public access point to the functionality.<br>
 <p>The grabber has got an interactive command-line menu through which it is possible to :</p>
 <ul>
    <li>parse list of CRC product pages for product variations with the required sizes and retrieve
    certain product variation with parameters and their values</li>
    <li>show results on the screen</li>
    <li>output results to csv files</li>
    <li>store results in the DB</li>
    <li>read/write configuration data from/to the text <code>[data/crc_*.cfg]</code> and binary <code>[data/crc_*.dat]</code> files</li>
    <li>performs exception handling for an incorrect input and missing data</li>
    <li>query the database and print out :
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
 <p>
 <b>Product sizes to search for data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_sizes.*]</code> files that contain
 set of comma separated sizes in double quotes, each type on a separate line.
 </p>
 <p>
 <b>Product parameters to retrieve data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_pars.*]</code> files that contain
 sets of parameter names to retrieve, in the same order as the order of size types in <code>[data/crc_sizes.*]</code>.
 </p>
 <p>
 <b>Product common parameters to retrieve data</b><br>
 Located in <code>[data]</code> folder, <code>[crc_search.*]</code> files that contain
 CSS or jQuery-like selectors to look for common product parameters on the pages.
 </p>
 
 <h3>Structure</h3>
 <p>
 <b>Databases</b><br>
 Located in <code>[db]</code> folder, <code>[crc.db]</code> and <code>[test.db]</code> database files for production and test purposes respectively.
 </p>
<p>
 <b>Output</b><br>
 Created csv files stored in <code>[output]</code> folder.
 </p>
 <p>
  <b>Test</b><br>
 Some classes have JUnit test suites. The <code>[test]</code> folder is required for some tests.
 </p>
 
 <h3>Usage</h3>
 <ol>
 <li>Update configuration files with the info that corresponding to your needs:
     <ul style="list-style-type: circle;">
        <li>Products data - <code>[data/crc_links.cfg]</code> - with products you are interested in <i>(most likely required)</i></li>
        <li>Product sizes to search for - <code>[data/crc_sizes.cfg]</code> - with product sizes you are interested in <i>(most likely required)</i></li>
        <li>Product parameters - <code>[data/crc_pars.cfg]</code> - with product parameters you are interested in <i>(most likely NOT required)</i></li>
        <li>Product common parameters - <code>[data/crc_search.cfg]</code> - with common parameters you are interested in <i>(most likely NOT required)</i></li>
    </ul>
 </li>
 <li>Run the app on a machine connected to the Internet and choose options you want from the menu (see the list on the top).</li>
 </ol>
 <p>
 @author S.V.
 @version 1.1.0
 @since 2021-03-14
 </p>
