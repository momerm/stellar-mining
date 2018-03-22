package main;

import org.apache.commons.codec.binary.Hex;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class MiningDB {
    private Connection conn;

    public static final String TABLE_ED25519 = "sigEd25519";
    public static final String TABLE_HASH_X = "sigHashX";
    public static final String TABLE_PRE_AUTH_TX = "sigPreAuthTx";
    public static final String TABLE_UNKNOWN = "sigUnknown"; // Unable to find signer key

    public MiningDB(String url, String username, String password) throws SQLException {
        conn = DriverManager.getConnection(url, username, password);
        conn.setAutoCommit(false);
    }

    public void createTableEd25519() throws SQLException {
        String query = "CREATE TABLE " + TABLE_ED25519 + " (\n" +
                        "id SERIAL PRIMARY KEY,\n" +
                        "txid CHAR(64) NOT NULL,\n" +
                        "r CHAR(44) NOT NULL,\n" +
                        "s CHAR(44) NOT NULL,\n" +
                        "accountid VARCHAR(56),\n" +
                        "signerkey VARCHAR(56)\n" +
                        ");";

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        conn.commit();
        stmt.close();
    }

    public void createTableHashX() throws SQLException {
        String query = "CREATE TABLE " + TABLE_HASH_X + " (\n" +
                "id SERIAL PRIMARY KEY,\n" +
                "txid CHAR(64) NOT NULL,\n" +
                "signature CHAR(88) NOT NULL,\n" +
                "accountid VARCHAR(56),\n" +
                "signerkey VARCHAR(56)\n" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        conn.commit();
        stmt.close();
    }

    public void createTablePreAuthTx() throws SQLException {
        String query = "CREATE TABLE " + TABLE_PRE_AUTH_TX + " (\n" +
                "id SERIAL PRIMARY KEY,\n" +
                "txid CHAR(64) NOT NULL,\n" +
                "signature CHAR(88) NOT NULL,\n" +
                "accountid VARCHAR(56),\n" +
                "signerkey VARCHAR(56)\n" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        conn.commit();
        stmt.close();
    }

    public void createTableUnknown() throws SQLException {
        String query = "CREATE TABLE " + TABLE_UNKNOWN + " (\n" +
                "id SERIAL PRIMARY KEY,\n" +
                "txid CHAR(64) NOT NULL,\n" +
                "signature CHAR(88) NOT NULL,\n" +
                "accountid VARCHAR(56)\n" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        conn.commit();
        stmt.close();
    }

    public void clearTable(String table) throws SQLException {
        String query = "DELETE FROM " + table + ";";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        conn.commit();
        stmt.close();
    }

    public void insertSignatureEvents(List<SignatureEvent> events) throws SQLException {
        Statement stmt = conn.createStatement();
        String sql = null;

        for(SignatureEvent e : events) {
            String txId_hex = Hex.encodeHexString(e.getTxId());

            String accountID = e.getAccountIDstr();

            if(e.getSignerKey() == null) {
                String signature_base64 = Base64.getEncoder().encodeToString(e.getSignature());
                sql = "INSERT INTO " + TABLE_UNKNOWN +
                        " VALUES (DEFAULT,"
                        + "'" + txId_hex + "',"
                        + "'" + signature_base64 + "',"
                        + "'" + accountID + "');";
            }
            else {
                String signerKey_base32 = e.getSignerKeyStr();
                switch (e.getSignerKey().getDiscriminant()) {
                    case SIGNER_KEY_TYPE_ED25519: {
                        byte[] r = Arrays.copyOfRange(e.getSignature(), 0, 32);
                        byte[] s = Arrays.copyOfRange(e.getSignature(), 32, 64);
                        String r_base64 = Base64.getEncoder().encodeToString(r);
                        String s_base64 = Base64.getEncoder().encodeToString(s);

                        sql = "INSERT INTO " + TABLE_ED25519 +
                                " VALUES (DEFAULT,"
                                + "'" + txId_hex + "',"
                                + "'" + r_base64 + "',"
                                + "'" + s_base64 + "',"
                                + "'" + accountID + "',"
                                + "'" + signerKey_base32 + "');";
                        break;
                    }
                    case SIGNER_KEY_TYPE_PRE_AUTH_TX: {
                        String signature_base64 = Base64.getEncoder().encodeToString(e.getSignature());
                        sql = "INSERT INTO " + TABLE_PRE_AUTH_TX +
                                " VALUES (DEFAULT,"
                                + "'" + txId_hex + "',"
                                + "'" + signature_base64 + "',"
                                + "'" + accountID + "',"
                                + "'" + signerKey_base32 + "');";
                        break;
                    }
                    case SIGNER_KEY_TYPE_HASH_X: {
                        String signature_base64 = Base64.getEncoder().encodeToString(e.getSignature());
                        sql = "INSERT INTO " + TABLE_HASH_X +
                                " VALUES (DEFAULT,"
                                + "'" + txId_hex + "',"
                                + "'" + signature_base64 + "',"
                                + "'" + accountID + "',"
                                + "'" + signerKey_base32 + "');";
                        break;
                    }
                }
            }
            stmt.addBatch(sql);
        }

        stmt.executeBatch();
        conn.commit();
        stmt.close();
    }

    public ResultSet getBadRandoms() throws SQLException {
        // Find signatures that use the same random.
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        String sql = "SELECT r, COUNT(r)\n" +
                "FROM " + TABLE_ED25519 + "\n" +
                "GROUP BY r\n" +
                "HAVING COUNT(r) > 1;";
        return stmt.executeQuery(sql);
    }


    public BadRandom getBadRandomUses(String random) throws SQLException {
        BadRandom badRandom = new BadRandom(random);
        byte[] r = Base64.getDecoder().decode(random);

        Statement stmt = conn.createStatement();
        String sql = "SELECT * FROM " + TABLE_ED25519 + "\n" +
                "WHERE r='" + random + "';";
        ResultSet resultSet = stmt.executeQuery(sql);

        while (resultSet.next()) {
            // Reconstruct signature from r and s
            byte[] s = Base64.getDecoder().decode(resultSet.getString(3));
            byte[] signature = new byte[r.length + s.length];
            System.arraycopy(r,  0, signature, 0, r.length);
            System.arraycopy(r,  0, signature, r.length, s.length);
            String signature_base64 = Base64.getEncoder().encodeToString(signature);

            badRandom.addUse(new BadRandom.BadRandomContext(
                    resultSet.getString(2),
                    signature_base64,
                    resultSet.getString(5),
                    resultSet.getString(6)));
        }
        resultSet.close();
        stmt.close();
        return badRandom;
    }

    public ResultSet getRepeatedSignerKeys() throws SQLException {
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        String sql = "SELECT signerkey, COUNT(signerkey) FROM " + TABLE_ED25519 + "\n" +
                "GROUP BY signerkey\n" +
                "HAVING COUNT(signerkey) > 1000;";
        return stmt.executeQuery(sql);
    }

    // Get all accounts that signed a transaction using the specified ed25519 key.
    public List<String> getAccountsBySignerKey(String signerKey) throws SQLException {
        Statement stmt = conn.createStatement();
        String sql = "SELECT accountid FROM " + TABLE_ED25519 + "\n" +
                "WHERE signerkey='" + signerKey + "'\n" +
                "GROUP BY accountid;";

        List<String> accountIDs = new ArrayList<>();

        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            accountIDs.add(rs.getString(1));
        }
        rs.close();
        stmt.close();

        return accountIDs;
    }

    public void close() throws SQLException {
        conn.close();
    }
}

