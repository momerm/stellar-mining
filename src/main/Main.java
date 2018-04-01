package main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    private static char getChar() throws IOException {
        char c, ignore;
        c = (char) System.in.read();

        do {
            ignore = (char) System.in.read();
        } while (ignore != '\n');

        return c;
    }

    private static boolean getYesNo() throws IOException {
        char c, ignore;

        do {
            c = (char) System.in.read();

            do {
                ignore = (char) System.in.read();
            } while (ignore != '\n');
        } while (c != 'y' && c != 'n');

        return c == 'y';
    }

    private static void createMiningDB(String dbUrl, String dbUser, String dbPass) throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl + "stellar", dbUser, dbPass);
        Statement stmt = conn.createStatement();
        String sql = "CREATE DATABASE mining;";
        stmt.executeUpdate(sql);
        System.out.println("Database \"mining\" created successfully.");
        conn.close();

        MiningDB miningDB = new MiningDB(dbUrl + "mining", dbUser, dbPass);
        miningDB.createTableEd25519();
        miningDB.createTableHashX();
        miningDB.createTablePreAuthTx();
        miningDB.createTableUnknown();
    }

    private static void dropMiningDB(String dbUrl, String dbUser, String dbPass) throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl + "stellar", dbUser, dbPass);
        Statement stmt = conn.createStatement();
        String sql = "DROP DATABASE mining;";
        stmt.executeUpdate(sql);
        System.out.println("Database \"mining\" deleted successfully.");
        conn.close();
    }

    private static void promptCreateMiningDB(String dbUrl, String dbUser, String dbPass) throws IOException {
        try {
            createMiningDB(dbUrl, dbUser, dbPass);
        } catch (SQLException e) {
            if(e.getMessage().compareTo("ERROR: database \"mining\" already exists") == 0) {
                System.out.print("Database \"mining\" already exists. Overwrite? (y/n): ");
                boolean c = getYesNo();
                if(c) {
                    try {
                        dropMiningDB(dbUrl, dbUser, dbPass);
                        createMiningDB(dbUrl, dbUser, dbPass);
                    }
                    catch (SQLException e1) {
                        System.out.println("Failed to create mining database");
                        e1.printStackTrace();
                    }
                }
            } else {
                System.out.println("Failed to create mining database");
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) throws IOException {
        if(args.length != 3) {
            System.out.println("Usage\n");
            System.out.println("java -jar stellar_miner.jar db_address db_username db_password\n");
            System.out.println("Example:");
            System.out.println("\tjava -jar stellar_miner.jar 127.0.0.1:5432 dbuser stellar\n");
            return;
        }

        String dbAddr = args[0];
        String dbUser = args[1];
        String dbPass = args[2];
        String dbUrl = "jdbc:postgresql://" + dbAddr + "/";

        // Main program
        char choice;
        for (;;) {
            do {
                System.out.println("               Stellar data mining v0.2           ");
                System.out.println("==================================================");
                System.out.println("  1. Create mining database");
                System.out.println("  2. Mine digital signature events");
                System.out.println("  3. Create bad randoms HTML file");
                System.out.println("  4. Create repeated signer keys HTML file");
                System.out.println("  5. Dump repeated signer keys (>100 times) and meta data to JSON file.\n");
                System.out.print("Choose one (q to quit): ");

                choice = getChar();

            } while (choice < '1' || choice > '5' && choice != 'q');

            if (choice == 'q') break;

            System.out.println("\n");

            switch (choice) {
                case '1':
                    // Create mining database
                    promptCreateMiningDB(dbUrl, dbUser, dbPass);
                    break;
                case '2':
                    // Mine digital signature events
                    SignatureMiner miner = new SignatureMiner(dbUrl, dbUser, dbPass);
                    miner.mineSignatures();
                    break;
                case '3':
                    // Create bad randoms HTML file
                    BadRandomHtmlBuilder badRandomHtmlBuilder = new BadRandomHtmlBuilder(dbUrl, dbUser, dbPass);
                    badRandomHtmlBuilder.buildHTML();
                    break;
                case '4':
                    // Create repeated signer keys HTML file
                    RepeatedSignerKeyHtmlBuilder htmlBuilder = new RepeatedSignerKeyHtmlBuilder(dbUrl, dbUser, dbPass);
                    htmlBuilder.buildHTML();
                    break;
                case '5':
                    // Dump all repeated signer keys and meta data to JSON file.
                    RepeatedSignerKeyJSONBuilder jsonBuilder = new RepeatedSignerKeyJSONBuilder(dbUrl, dbUser, dbPass);
                    jsonBuilder.buildJSON();
                    break;
            }
            System.out.println();
        }
    }
}
