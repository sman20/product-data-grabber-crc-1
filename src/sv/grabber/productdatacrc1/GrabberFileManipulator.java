package sv.grabber.productdatacrc1;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of all necessary methods to interact with files on disk.
 */
public class GrabberFileManipulator {

    private static final String LINKS_FILE = "data/crc_links.cfg";
    private static final String SIZES_FILE = "data/crc_sizes.cfg";
    private static final String PARS_FILE = "data/crc_pars.cfg";
    private static final String SEARCHES_FILE = "data/crc_search.cfg";

    private static final String LINKS_BINFILE = "data/crc_links.dat";
    private static final String SIZES_BINFILE = "data/crc_sizes.dat";
    private static final String PARS_BINFILE = "data/crc_pars.dat";
    private static final String SEARCHES_BINFILE = "data/crc_search.dat";

    static final String ITEM_VARIANTS_SCRIPT = "allVariants";

    static String getItemVariantsScript() {
        return ITEM_VARIANTS_SCRIPT;
    }

    // file reader, NIO version
    private static ArrayList<String[]> getDataFromFileNIO (String fileName) {
        ArrayList<String[]> fileData = new ArrayList<>();
        Path path = Paths.get(System.getProperty("user.dir"), fileName);
        try {
            List<String> stringList = Files.readAllLines(path);
            for (String line : stringList) {
                String[] lineItems = line.split(", ");
                if (!lineItems[0].startsWith("//")) fileData.add(lineItems);     // to ignore commented out
            }
        } catch (IOException e) {
            System.out.println("Error reading " + path + " : " + e);
            e.printStackTrace();
        }
        return fileData;
    }

    // binary file reader
    private static ArrayList<String[]> getDataFromBinaryFile (String fileName) {
        ArrayList<String[]> binFileData = new ArrayList<>();
        try (DataInputStream dataInputStream = new DataInputStream( new BufferedInputStream(new FileInputStream(fileName)) )){
            boolean eof = false;
            while (!eof) {
                StringBuilder dataLine = new StringBuilder();
                try {
                    while (true) {
                        String dataItem = dataInputStream.readUTF();
                        if (dataLine.toString().equals("")) {
                            dataLine.append(dataItem);
                        } else {
                            if (!dataItem.equals("\n")) {
                                dataLine.append(", ");
                                dataLine.append(dataItem);
                            } else {
                                break;
                            }
                        }
                    }
                    binFileData.add(dataLine.toString().split(", "));
                } catch (EOFException e) {
                    eof = true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error reading " + fileName + " : " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error reading from data stream for " + fileName + " : " + e);
            e.printStackTrace();
        }
        return binFileData;
    }

    // file writer, IO ver.
    static void putDataToFile (String fileName, ArrayList<String[]> data) {
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (String[] line : data) {
                for (String dataItem : line) {
                    fileWriter.write(dataItem);
                    if (!dataItem.equals(line[line.length - 1])) {
                        fileWriter.write(", ");
                    }
                }
                fileWriter.write("\n");
            }
        } catch (IOException e) {
            System.out.println("Error writing " + fileName + " : " + e);
            e.printStackTrace();
        }
    }

    // binary file writer
    static void putDataToBinaryFile (String fileName, ArrayList<String[]> data) {
        try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            for (String[] line : data) {
                for (String dataItem : line) {
                    dataOutputStream.writeUTF(dataItem);
                }
                dataOutputStream.writeUTF("\n");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error writing " + fileName + " : " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error writing to data stream for " + fileName + " : " + e);
            e.printStackTrace();
        }
    }

    // file writer for results, NIO ver.
    static String storeDataCsvFile(String namePrefix, StringBuffer data) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmssMs");
        Date dateNow = new Date();
        String timeStamp = dateFormat.format(dateNow);
        String outputFileName = "output/crc_" + namePrefix + "." + timeStamp + "." + ((int) (Math.random()*100)) + ".csv";
        Path path = Paths.get(System.getProperty("user.dir"), outputFileName);
        try {
            Files.write(path, data.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Error writing " + path + " : " + e);
            e.printStackTrace();
        }
        return outputFileName;
    }

    static ArrayList<String[]> getCfgFromCfgFile(String cfgFile) {
        ArrayList<String[]> data = new ArrayList<>();
        Path path = Paths.get(System.getProperty("user.dir"), cfgFile);
        if (Files.exists(path))
            data = getDataFromFileNIO(cfgFile);
        else {
            System.out.println("No CFG file found: " + cfgFile);
        }
        return data;
    }
    static ArrayList<String[]> getCfgFromDatFile(String datFile) {
        ArrayList<String[]> data = new ArrayList<>();
        Path pathBin = Paths.get(System.getProperty("user.dir"), datFile);
        if (Files.exists(pathBin))
            data = getDataFromBinaryFile(datFile);
        else {
            System.out.println("No DAT file found: " + datFile);
        }
        return data;
    }
    static ArrayList<String[]> getLinksFromCfgFile() {
        return getCfgFromCfgFile(LINKS_FILE);
    }
    static ArrayList<String[]> getLinksFromDatFile() {
        return getCfgFromDatFile(LINKS_BINFILE);
    }
    static ArrayList<String[]> getSizesFromCfgFile() {
        return getCfgFromCfgFile(SIZES_FILE);
    }
    static ArrayList<String[]> getSizesFromDatFile() {
        return getCfgFromDatFile(SIZES_BINFILE);
    }
    static ArrayList<String[]> getParsFromCfgFile() {
        return getCfgFromCfgFile(PARS_FILE);
    }
    static ArrayList<String[]> getParsFromDatFile() {
        return getCfgFromDatFile(PARS_BINFILE);
    }
    static ArrayList<String[]> getPageItemsFromCfgFile() {
        return getCfgFromCfgFile(SEARCHES_FILE);
    }
    static ArrayList<String[]> getPageItemsFromDatFile() {
        return getCfgFromDatFile(SEARCHES_BINFILE);
    }

    static void putLinksToCfgFile(ArrayList<String[]> links) {
        putDataToFile(LINKS_FILE, links);
    }
    static void putLinksToDatFile(ArrayList<String[]> links) {
        putDataToBinaryFile(LINKS_BINFILE, links);
    }
    static void putSizesToCfgFile(ArrayList<String[]> sizes) {
        putDataToFile(SIZES_FILE, sizes);
    }
    static void putSizesToDatFile(ArrayList<String[]> sizes) {
        putDataToBinaryFile(SIZES_BINFILE, sizes);
    }
    static void putParsToCfgFile(ArrayList<String[]> pars) {
        putDataToFile(PARS_FILE, pars);
    }
    static void putParsToDatFile(ArrayList<String[]> pars) {
        putDataToBinaryFile(PARS_BINFILE, pars);
    }
    static void putPageItemsToCfgFile(ArrayList<String[]> pageItems) {
        putDataToFile(SEARCHES_FILE, pageItems);
    }
    static void putPageItemsToDatFile(ArrayList<String[]> pageItems) {
        putDataToBinaryFile(SEARCHES_BINFILE, pageItems);
    }
}
