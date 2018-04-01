package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RepeatedSignerKeyHtmlBuilder {
    private MiningDB miningDB = null;

    public RepeatedSignerKeyHtmlBuilder(String url, String username, String password) {
        try {
            miningDB = new MiningDB(url + "mining", username, password);
        }
        catch (SQLException e) {
            miningDB = null;
            System.out.println("Failed to connect to mining database.");
            e.printStackTrace();
        }
    }

    public void buildHTML() {
        if(miningDB == null) return;
        System.out.println("Building repeated signer keys HTML page...");

        try(BufferedWriter writer = new BufferedWriter(new FileWriter("repeatedsignerkeys.html"))) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("<meta charset=\"UTF-8\">\n");
            writer.write("<title>Frequently Used Signer Keys in the Stellar Ledger</title>\n");
            writer.write("<style>\n" +
                    "td {\n" +
                    "\tpadding: 5px;\n" +
                    "}\n" +
                    ".left-align {\n" +
                    "\ttext-align: left;\n" +
                    "}\n" +
                    "</style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            writer.write("<h1>Signer Keys used over 1000 times in the Stellar Ledger</h1>\n");

            LocalDate dateTime = LocalDate.now();
            writer.write("<p> Generated on " + dateTime.toString() + ".</p>\n");

            ResultSet rs = miningDB.getRepeatedSignerKeys(1000);

            int nKeys = miningDB.getRepeatedSignerKeyCount(1000);

            writer.write("<h3>" + nKeys + " keys found</h3>\n");

            writer.write("<table>\n");

            writer.write("<thead>\n");
            writer.write("<tr>\n");
            writer.write("<th>Count</th>\n");
            writer.write("<th>Signer Key</th>\n");
            writer.write("<th class=\"left-align\">Account IDs</th>\n");
            writer.write("</tr>\n");
            writer.write("</thead>\n");

            writer.write("<tbody>\n");

            // Add signer keys to HTML page
            // Rows for count SignerKey [AccountIDs]
            while(rs.next()) {
                String signerKey = rs.getString(1);
                int count = rs.getInt(2);

                writer.write("<tr>\n");

                // Count
                writer.write("<td>");
                writer.write(Integer.toString(count));
                writer.write("</td>\n");

                // Signer Key
                writer.write("<td>");
                writer.write(signerKey);
                writer.write("</td>\n");

                // [AccountIDs]
                writer.write("<td>");
                writer.write("[");

                List<String> accountIDs = miningDB.getAccountsBySignerKey(signerKey);
                if(accountIDs.size() > 0) {
                    writer.write("<a href=\"https://horizon.stellar.org/accounts/" + accountIDs.get(0) + "\" target=\"_blank\">" + accountIDs.get(0) + "</a>");
                }
                for (int i = 1; i < accountIDs.size(); i++) {
                    String accountID = accountIDs.get(i);
                    writer.write(",&nbsp;");
                    writer.write("<a href=\"https://horizon.stellar.org/accounts/" + accountID + "\" target=\"_blank\">" + accountID + "</a>");
                }

                writer.write("]");
                writer.write("</td>\n");
                writer.write("</tr>\n");
            }

            rs.getStatement().close();
            rs.close();


            writer.write("</tbody>\n");
            writer.write("</table>\n");
            writer.write("</body>\n");
            writer.write("</html>");

            System.out.println("Done");
        } catch (IOException e) {
            System.out.println("Failed to write to repeatedsignerkeys.html");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error getting signer keys from database.");
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
