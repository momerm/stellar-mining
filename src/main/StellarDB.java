package main;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StellarDB {
    private Connection conn;

    public StellarDB(String url, String username, String password) throws SQLException {
        conn = DriverManager.getConnection(url, username, password);
    }

    public ResultSet getTransactions() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery("SELECT txid, txbody, txmeta FROM txhistory;");
    }

    /*
    public List<String> getAccountPublicKeys(String accountID) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT publickey FROM signers WHERE accountid = '" + accountID + "';");

        ArrayList<String> publicKeys = new ArrayList<>();
        while(rs.next())
            publicKeys.add(rs.getString(1));

        rs.close();
        stmt.close();
        return publicKeys;
    }*/

    public HashMap<String, List<String>> getAccountSigners(List<String> accountIDs) throws SQLException {
        StringBuilder accountIDstr = new StringBuilder();
        accountIDstr.append('(');
        for(int i = 0; i < accountIDs.size(); i++) {
            accountIDstr.append('\'');
            accountIDstr.append(accountIDs.get(i));
            accountIDstr.append('\'');
            if(i != accountIDs.size() - 1) {
                accountIDstr.append(',');
            }
        }
        accountIDstr.append(')');

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT accountid, publickey FROM signers WHERE accountid IN " + accountIDstr.toString() + ";");

        HashMap<String, List<String>> hashMap = new HashMap<>();

        while(rs.next()) {
            String accountID = rs.getString(1);
            String publicKey = rs.getString(2);
            if (!hashMap.containsKey(accountID)) {
                List<String> list = new ArrayList<>(5);
                list.add(publicKey);

                hashMap.put(accountID, list);
            } else {
                hashMap.get(accountID).add(publicKey);
            }
        }

        rs.close();
        stmt.close();
        return hashMap;
    }

    public void close() throws SQLException {
        conn.close();
    }
}

