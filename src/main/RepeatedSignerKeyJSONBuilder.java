package main;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class RepeatedSignerKeyJSONBuilder {
    private MiningDB miningDB = null;

    public RepeatedSignerKeyJSONBuilder(String url, String username, String password) {
        try {
            miningDB = new MiningDB(url + "mining", username, password);
        }
        catch (SQLException e) {
            miningDB = null;
            System.out.println("Failed to connect to mining database.");
            e.printStackTrace();
        }
    }

    public void buildJSON() {
        if(miningDB == null) return;
        System.out.println("Building repeated signer keys JSON file...");

        JsonFactory jFactory = new JsonFactory();

        try {
            JsonGenerator jGenerator = jFactory.createGenerator(new File("Repeated Signer Keys.json"), JsonEncoding.UTF8);

            jGenerator.writeStartObject();

            // Write number of repeated keys found
            ResultSet keysRs = miningDB.getRepeatedSignerKeys(100);
            int nKeys = miningDB.getRepeatedSignerKeyCount(100);

            jGenerator.writeNumberField("number of keys", nKeys);

            // Array for all signature events
            jGenerator.writeArrayFieldStart("repeated keys");

            int n = 0; // progress counter
            while (keysRs.next()) {
                jGenerator.writeStartObject();
                String signerKey = keysRs.getString(1);
                int count = keysRs.getInt(2);

                jGenerator.writeStringField("signer key", signerKey);
                jGenerator.writeNumberField("count", count);

                List<String> accountIDs = miningDB.getAccountsBySignerKey(signerKey);

                // Number of accounts that signed a transaction using this key
                jGenerator.writeNumberField("number of accounts", accountIDs.size());

                // Array of accounts that used this key to sign transactions
                jGenerator.writeArrayFieldStart("accounts");
                for(String accountID : accountIDs) {
                    jGenerator.writeString(accountID);
                }
                jGenerator.writeEndArray();

                // Array of transaction ids and signature pairs
                jGenerator.writeArrayFieldStart("transactions");

                ResultSet txRs = miningDB.getTxIdSignaturesBySignerKey(signerKey);
                while (txRs.next()) {
                    jGenerator.writeStartArray();
                    // Write txID
                    jGenerator.writeString(txRs.getString(1));

                    // Reconstruct signature from r and s.
                    byte[] r = Base64.getDecoder().decode(txRs.getString(2));
                    byte[] s = Base64.getDecoder().decode(txRs.getString(3));
                    byte[] signature_bytes = new byte[64];
                    for (int i=0; i < 32; i++) {
                        signature_bytes[i] = r[i];
                        signature_bytes[i+32] = s[i];
                    }
                    String signature_base64 = Base64.getEncoder().encodeToString(signature_bytes);

                    // Write signature
                    jGenerator.writeString(signature_base64);

                    jGenerator.writeEndArray();
                }
                txRs.getStatement().close();
                txRs.close();

                jGenerator.writeEndArray();
                jGenerator.writeEndObject();

                n++;
                System.out.printf("\r%f%%", (float)n / (float)nKeys * 100.f);
            }
            System.out.printf("\nDumped %d repeated keys\n", n);

            keysRs.getStatement().close();
            keysRs.close();

            jGenerator.writeEndArray();
            jGenerator.writeEndObject();
            jGenerator.close();

            System.out.println("Done!");
        } catch (Exception e) {
            System.out.println("Failed to create JSON file.");
            e.printStackTrace();
        }

        try {
            miningDB.close();
        } catch (SQLException e) {
            System.out.println("Failed to close database connection.");
            e.printStackTrace();
        }
    }

}
