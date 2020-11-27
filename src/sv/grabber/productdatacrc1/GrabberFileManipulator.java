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

    private static final String ITEM_VARIANTS_SCRIPT = "allVariants";

    public static String getItemVariantsScript() {
        return ITEM_VARIANTS_SCRIPT;
    }

    // NOTE it uses the old cached data when file is updated on the fly

    // file reader, IO version
    private static ArrayList<String[]> getDataFromFile (String fileName) {
        ArrayList<String[]> fileData = new ArrayList<>();
        try ( Scanner scanner = new Scanner(new BufferedReader(new FileReader(fileName))) ){
            while (scanner.hasNextLine()) {
                String[] cfgFileDataPar = scanner.nextLine().split(", ");
                if (!cfgFileDataPar[0].startsWith("//")) fileData.add(cfgFileDataPar);     // to ignore commented out
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error reading " + fileName + " : " + e);
            e.printStackTrace();
        }
        return fileData;
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
    private static void putDataToFile (String fileName, ArrayList<String[]> data) {
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
    private static void putDataToBinaryFile (String fileName, ArrayList<String[]> data) {
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
    public static String storeDataCsvFile(String prefix, StringBuffer data) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmssMs");
        Date dateNow = new Date();
        String timeStamp = dateFormat.format(dateNow);
        String outputFileName = "output/crc_" + prefix + "." + timeStamp + "." + ((int) (Math.random()*100)) + ".csv";
        Path path = Paths.get(System.getProperty("user.dir"), outputFileName);
        try {
            Files.write(path, data.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Error writing " + path + " : " + e);
            e.printStackTrace();
        }
        return outputFileName;
    }

    private static ArrayList<String[]> getCfgFromFile(boolean isFileBinary, String cfgFile, String datFile) {
        Path path = Paths.get(System.getProperty("user.dir"), cfgFile);
        Path pathBin = Paths.get(System.getProperty("user.dir"), datFile);
        if (!isFileBinary) {
            if (Files.exists(path)) return getDataFromFileNIO(cfgFile);
            else {
                System.out.println("No CFG file found: " + cfgFile);
                return null;
            }
        } else {
            if (Files.exists(pathBin)) return getDataFromBinaryFile(datFile);
            else {
                System.out.println("No DAT file found: " + datFile);
                return null;
            }
        }
    }
    public static ArrayList<String[]> getLinksFromFile(boolean isFileBinary) {
        return getCfgFromFile(isFileBinary, LINKS_FILE, LINKS_BINFILE);
    }
    public static ArrayList<String[]> getSizesFromFile(boolean isFileBinary) {
        return getCfgFromFile(isFileBinary, SIZES_FILE, SIZES_BINFILE);
    }
    public static ArrayList<String[]> getParsFromFile(boolean isFileBinary) {
        return getCfgFromFile(isFileBinary, PARS_FILE, PARS_BINFILE);
    }
    public static ArrayList<String[]> getPageItemsFromFile(boolean isFileBinary) {
        return getCfgFromFile(isFileBinary, SEARCHES_FILE, SEARCHES_BINFILE);
    }

    private static void putCfgToFile(String cfgFile, String datFile, ArrayList<String[]> data, boolean isFileBinary) {
        if (!isFileBinary) putDataToFile(cfgFile, data);
        else putDataToBinaryFile(datFile, data);
    }
    public static void putLinksToFile(ArrayList<String[]> links, boolean isFileBinary) {
        putCfgToFile(LINKS_FILE, LINKS_BINFILE, links, isFileBinary);
    }
    public static void putSizesToFile(ArrayList<String[]> sizes, boolean isFileBinary) {
        putCfgToFile(SIZES_FILE, SIZES_BINFILE, sizes, isFileBinary);
    }
    public static void putParsToFile(ArrayList<String[]> pars, boolean isFileBinary) {
        putCfgToFile(PARS_FILE, PARS_BINFILE, pars, isFileBinary);
    }
    public static void putPageItemsToFile(ArrayList<String[]> pageItems, boolean isFileBinary) {
        putCfgToFile(SEARCHES_FILE, SEARCHES_BINFILE, pageItems, isFileBinary);
    }

}
