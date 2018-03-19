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
                    "</style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            writer.write("<h1>Signer Keys used over 1000 times in the Stellar Ledger</h1>\n");

            LocalDate dateTime = LocalDate.now();
            writer.write("<p> Generated on " + dateTime.toString() + ".</p>\n");

            ResultSet rs = miningDB.getRepeatedSignerKeys();

            int nKeys = 0;
            if(rs.last()) {
                nKeys = rs.getRow();
                rs.beforeFirst();
            }

            writer.write("<h3>" + nKeys + " keys found</h3>\n");

            writer.write("<table>\n");

            writer.write("<thead>\n");
            writer.write("<tr>\n");
            writer.write("<th>Count</th>\n");
            writer.write("<th>Signer Key</th>\n");
            writer.write("<th>Account IDs</th>\n");
            writer.write("</tr>\n");
            writer.write("</thead>\n");

            writer.write("<tbody>\n");

            // Add signer keys to HTML page
            // Rows for count SignerKey [AccountIDs]
            while(rs.next()) {
                String signerKey = rs.getString(1);
                int count = rs.getInt(2);
                List<String> accountIDs = miningDB.getAccountsBySignerKey(signerKey);

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
                for(int i = 0; i < accountIDs.size(); i++) {
                    writer.write("<a href=\"https://horizon.stellar.org/accounts/" + accountIDs.get(i) + "\" target=\"_blank\">" + accountIDs.get(i) + "</a>");
                    if (i < accountIDs.size() - 1) writer.write(",&nbsp;");
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
