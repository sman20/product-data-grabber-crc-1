package sv.grabber.productdatacrc1;

import java.util.Scanner;

public class Main {

    private static Scanner inputReader = new Scanner(System.in);
    private static PageDataGrabber parser = new PageDataGrabber();

    public static void main(String[] args) {

//        parser.setDebugMode(true);
        boolean toExit = false;
        printMenu();
        while (!toExit) {
            switch (getIntInput()) {
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
                    parser.printCollectedDbData();
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
                    toExit = true;
                    break;
                default:
                    printMenu();
                    break;
            }
            printMenuShort();
        }
    }

    private static int getIntInput() {
        System.out.print("Enter your choice : ");
        int choice = inputReader.nextInt();
        inputReader.nextLine();                 // NOTE, needed to finalize input
        return choice;
    }

    private static void printMenu() {
        System.out.println("~~~~~ page parser [v." + parser.getVersion() + "] ~~~~~");
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
        System.out.println("any_other - print menu");
    }
    private static void printMenuShort() {
        System.out.print("(0) show current config | ");
        System.out.println("parse pages and (1) show results " +
                                         "(2) show detailed " +
                                         "(3) save to csv " +
                                         "(4) save to db");
        System.out.print("(5) show results from db | ");
        System.out.print("(6) get config from CFG files | ");
        System.out.print("(7) put config to CFG files | ");
        System.out.println("(8) get config from DAT files");
        System.out.print("(other_nmb) print menu | ");
        System.out.println("(9) - exit and put current config in DAT files");
    }
}
