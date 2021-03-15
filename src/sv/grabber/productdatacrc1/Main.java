package sv.grabber.productdatacrc1;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Entry point to the Product Data Grabber that provides a interactive menu to parse list of CRC product pages
 * for product variations, show results, output results to csv files, store results in the DB,
 * read/write configuration data from/to the text and binary files and also query the database for a particular product
 * (see {@link ProductDataGrabber}).
 */

public class Main {

    private static ProductDataGrabber parser = new ProductDataGrabber();

    public static void main(String[] args) {

//        parser.setDebugMode(true);
        boolean toExit = false;
        printMenu();
        try (Scanner inputReader = new Scanner(System.in)) {
            while (!toExit) {
                boolean toPrintShortMenu = true;
                switch (getIntInput(inputReader)) {
                    default:
                        printMenu();
                        toPrintShortMenu = false;
                        break;
                    case 0:
                        parser.printCurrentData();
                        break;
                    case 1:
                        parser.parsePagesAndShowBrief();
                        break;
                    case 2:
                        parser.parsePagesAndShowDetailed();
                        break;
                    case 3:
                        parser.parsePagesAndToCsv();
                        break;
                    case 4:
                        parser.parsePagesAndToDb();
                        break;
                    case 5:
                        parser.printAllCollectedDbData();
                        break;
                    case 6:
                        parser.getConfigurationFromCfgFiles();
                        break;
                    case 7:
                        parser.putConfigurationToCfgFiles();
                        break;
                    case 8:
                        parser.getConfigurationFromDatFiles();
                        break;
                    case 9:
                        parser.putConfigurationToDatFiles();
                        inputReader.close();
                        toPrintShortMenu = false;
                        toExit = true;
                        break;
                    case 10:
                        parser.printChangedDbData();
                        break;
                    case 11:
                        parser.printRecentlyChangedDbData();
                        break;
                    case 12:
                        parser.printParticularProductDbData(getStringInput(inputReader, "Product SKUID"));
                        break;
                    case 13:
                        parser.printNamedAlikeProductsDbData(getStringInput(inputReader, "Partial or full product name"));
                        break;
                }
                if (toPrintShortMenu)
                    printMenuShort();
            }
        } catch (InputMismatchException imX) {
            System.out.println("ERROR - Numeric input expected. " + imX);
        }
    }

    private static int getIntInput(Scanner intReader) {
        System.out.print("Enter your choice : ");
        int choice = intReader.nextInt();
        intReader.nextLine();
        return choice;
    }
    private static String getStringInput(Scanner stringReader, String what) {
        System.out.print("Enter " + what + " : ");
        return stringReader.nextLine();
    }
    private static void printMenu() {
        System.out.println("~~~~~ Product data parser [v." + parser.getVersion() + "] ~~~~~");
        System.out.println("0 - show current configuration (links, sizes, search data, parameters)");
        System.out.println("1 - parse pages and show results");
        System.out.println("2 - parse pages and show detailed results");
        System.out.println("3 - parse pages and save results TO CSV files");
        System.out.println("4 - parse pages and save results TO DB");
        System.out.println("5 - show results history FROM DB");
        System.out.println("6 - download configuration FROM CFG files");
        System.out.println("7 - upload configuration TO CFG files");
        System.out.println("8 - download configuration FROM DAT files");
        System.out.println("9 - exit and store current configuration in DAT files");
        System.out.println("10 - show results history FROM DB that CHANGED");
        System.out.println("11 - show results history FROM DB that RECENTLY CHANGED");
        System.out.println("12 - show results history FROM DB for a product (SKU ID)");
        System.out.println("13 - show results history FROM DB for named alike products (full or partial name)");
        System.out.println("any_other - print menu");
    }
    private static void printMenuShort() {
        System.out.print("(0) show current config | ");
        System.out.println("parse pages and (1)-show results " +
                "(2)-show detailed " +
                "(3)-save to csv " +
                "(4)-save to db");
        System.out.println("(5) show results from db | (10) show changed results from db | (11) show recently changed results from db");
        System.out.println("(12) - show results history FROM DB for a product | (13) show results history FROM DB for named alike products");
        System.out.print("(6) get config from CFG files | ");
        System.out.print("(7) put config to CFG files | ");
        System.out.println("(8) get config from DAT files");
        System.out.print("(9) exit and put current config in DAT files | ");
        System.out.println("(other_nmb) print the menu");
    }
}
