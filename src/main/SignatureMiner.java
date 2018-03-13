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
            miningDB.dropTableEd25519();
            miningDB.dropTableHashX();
            miningDB.dropTablePreAuthTx();
            miningDB.dropTableUnknown();

            miningDB.createTableEd25519();
            miningDB.createTableHashX();
            miningDB.createTablePreAuthTx();
            miningDB.createTableUnknown();
        }
        catch (SQLException e) {
            System.out.println("Failed to create tables in mining database.");
            e.printStackTrace();
            return;
        }

        // Extract signature events from transactions
        int n = 0; // Transaction count
        int m = 0; // Signature count
        List<SignatureEvent> allSignatureEvents = new ArrayList<>(520);
        try {
            ResultSet txResultSet = stellarDB.getTransactions();
            while (txResultSet.next()) {
                String txId_hex = txResultSet.getString(1);
                String txBody_base64 = txResultSet.getString(2);
                String txMeta_base64 = txResultSet.getString(3);

                List<SignatureEvent> signatureEvents = TransactionParser.getSignatureEvents(txId_hex, txBody_base64, txMeta_base64, stellarDB);
                allSignatureEvents.addAll(signatureEvents);
                m += signatureEvents.size();

                if(allSignatureEvents.size() > 500) {
                    miningDB.insertSignatureEvents(allSignatureEvents);
                    allSignatureEvents.clear();
                }

                System.out.printf("\rProcessed %d transactions", ++n);
            }
            txResultSet.close();

            if(allSignatureEvents.size() > 0)
                miningDB.insertSignatureEvents(allSignatureEvents);

            System.out.println("\nFound " + m + " signatures.");
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
