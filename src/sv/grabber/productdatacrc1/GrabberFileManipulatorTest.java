package sv.grabber.productdatacrc1;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GrabberFileManipulatorTest {

    private static final String TEST_CFG_FILE = "test/test.cfg";
    private static final String TEST_DAT_FILE = "test/test.dat";

    private static final String TEST_PRODUCTS = "Product 1 Wrench, One Size, , One Size, €11, Black, sku000001\nProduct 2 Shoes, US 7.5, , , €22, Silver, sku000002\nProduct 3 Shorts, M, , , €33, Bronze, sku000003\nProduct 4 Tire, 26\", 2.10, Folding Bead, €44, Black - Reflective, sku000004";
    private static final String TEST_CFG_CONTENT = "Product 1 Wrench, Size, https://www.site.com/1-wrench\nProduct 2 Shoes, ShoesSize, https://www.site.com/2-shoes\nProduct 3 Shorts, ClothingSize, https://www.site.com/3-shorts\nProduct 4 Tire, WheelSize, https://www.site.com/4-tire";

    @Test
    void getItemVariantsScript() {
        assertEquals(GrabberFileManipulator.ITEM_VARIANTS_SCRIPT, GrabberFileManipulator.getItemVariantsScript());
    }

    @Test
    void storeDataCsvFile() throws IOException {
        String csvFileName = GrabberFileManipulator.storeDataCsvFile("TEST", new StringBuffer(TEST_PRODUCTS));
        Path pathToCsvFile = Paths.get(csvFileName);
        assertEquals(TEST_PRODUCTS, Files.readString(pathToCsvFile));
        assertTrue(Files.deleteIfExists(pathToCsvFile));
    }

    @Test
    void putToAndGetFromCfgFile() throws IOException {
        Path pathToCfgFile = Paths.get(TEST_CFG_FILE);
        assertFalse(Files.deleteIfExists(pathToCfgFile));
        ArrayList<String[]> data = getListOfArrayStrings();
        GrabberFileManipulator.putDataToFile(TEST_CFG_FILE, data);
        ArrayList<String[]> actualResult = GrabberFileManipulator.getCfgFromCfgFile(TEST_CFG_FILE);
        assertEquals("[[Product 1 Wrench, Size, https://www.site.com/1-wrench], [Product 2 Shoes, ShoesSize, https://www.site.com/2-shoes], [Product 3 Shorts, ClothingSize, https://www.site.com/3-shorts], [Product 4 Tire, WheelSize, https://www.site.com/4-tire]]",
                Arrays.deepToString(actualResult.toArray()));
        assertTrue(Files.deleteIfExists(pathToCfgFile));
    }

    @Test
    void putToAndGetFromDatFile() throws IOException {
        Path pathToDatFile = Paths.get(TEST_DAT_FILE);
        assertFalse(Files.deleteIfExists(pathToDatFile));
        ArrayList<String[]> data = getListOfArrayStrings();
        GrabberFileManipulator.putDataToBinaryFile(TEST_DAT_FILE, data);
        ArrayList<String[]> actualResult = GrabberFileManipulator.getCfgFromDatFile(TEST_DAT_FILE);
        assertEquals("[[Product 1 Wrench, Size, https://www.site.com/1-wrench], [Product 2 Shoes, ShoesSize, https://www.site.com/2-shoes], [Product 3 Shorts, ClothingSize, https://www.site.com/3-shorts], [Product 4 Tire, WheelSize, https://www.site.com/4-tire]]",
                Arrays.deepToString(actualResult.toArray()));
        assertTrue(Files.deleteIfExists(pathToDatFile));
    }

    private ArrayList<String[]> getListOfArrayStrings() {
        ArrayList<String[]> data = new ArrayList<>();
        String[] lines = TEST_CFG_CONTENT.split("\n");
        for (String line : lines) {
            data.add(line.split(", "));
        }
        return data;
    }
}