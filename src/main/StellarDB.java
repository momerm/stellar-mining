package main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StellarDB {
    private Connection conn;

    public StellarDB(String url, String username, String password) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        conn = DriverManager.getConnection(url, username, password);
    }

    public ArrayList<String> getAccountPublicKeys(List<String> accountIDs) throws SQLException {
        Statement stmt = conn.createStatement();

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

        ResultSet rs = stmt.executeQuery("SELECT publickey FROM signers WHERE accountid IN " + accountIDstr.toString() + ";");
        ArrayList<String> publicKeys = new ArrayList<>();

        while(rs.next())
            publicKeys.add(rs.getString(1));

        return publicKeys;
    }

    public void close() throws SQLException {
        conn.close();
    }
}

