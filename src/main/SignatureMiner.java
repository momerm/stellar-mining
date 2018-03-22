package main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SignatureMiner {
    private StellarDB stellarDB = null;
    private MiningDB miningDB = null;

    public SignatureMiner(String url, String username, String password) {
        try {
            stellarDB = new StellarDB(url + "stellar", username, password);
        }
        catch (SQLException e) {
            stellarDB = null;
            System.out.println("Failed to connect to stellar database.");
            e.printStackTrace();
        }

        try {
            miningDB = new MiningDB(url + "mining", username, password);
        }
        catch (SQLException e) {
            try {
                stellarDB.close();
            } catch (SQLException e1) {
                System.out.println("Failed to close connection to stellar database.");
                e1.printStackTrace();
            }
            miningDB = null;
            System.out.println("Failed to connect to mining database.");
            e.printStackTrace();
        }
    }

    public void mineSignatures() {
        if(stellarDB == null || miningDB == null) return;
        System.out.println("Mining digital signatures...");
        // Create tables in mining database
        try {
            miningDB.clearTable(MiningDB.TABLE_ED25519);
            miningDB.clearTable(MiningDB.TABLE_HASH_X);
            miningDB.clearTable(MiningDB.TABLE_PRE_AUTH_TX);
            miningDB.clearTable(MiningDB.TABLE_UNKNOWN);
        }
        catch (SQLException e) {
            System.out.println("Failed to clear tables in mining database.");
            e.printStackTrace();
            return;
        }

        // Extract signature events from transactions
        int txCount = 0; // Number of transactions processed
        int sigCount = 0; // Number of signatures found

        List<SignatureEvent> allSignatureEvents = new ArrayList<>(10030);
        try {
            ResultSet rs = stellarDB.getTransactions();
            while (rs.next()) {
                String txId_hex = rs.getString(1);
                String txBody_base64 = rs.getString(2);
                String txMeta_base64 = rs.getString(3);

                List<SignatureEvent> signatureEvents = TransactionParser.getSignatureEvents(txId_hex, txBody_base64, txMeta_base64, stellarDB);
                allSignatureEvents.addAll(signatureEvents);
                sigCount += signatureEvents.size();

                if(allSignatureEvents.size() > 10000) {
                    miningDB.insertSignatureEvents(allSignatureEvents);
                    allSignatureEvents.clear();
                }

                txCount++;
                System.out.printf("\rProcessed %d Transactions. Found %d signatures.", txCount, sigCount);
            }
            rs.getStatement().close();
            rs.close();

            if(allSignatureEvents.size() > 0)
                miningDB.insertSignatureEvents(allSignatureEvents);

            System.out.println("\nFound " + sigCount + " signatures.");
            System.out.println("\nDone");
        } catch (Exception e) {
            System.out.println("Error processing transactions.");
            e.printStackTrace();
        }

        try {
            stellarDB.close();
            miningDB.close();
        } catch (SQLException e) {
            System.out.println("Failed to close database connection.");
            e.printStackTrace();
        }
    }
}
