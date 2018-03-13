package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class BadRandomHtmlBuilder {
    private MiningDB miningDB = null;

    public BadRandomHtmlBuilder(String url, String username, String password) {
        try {
            miningDB = new MiningDB(url + "mining", username, password);
        }
        catch (SQLException e) {
            miningDB = null;
            System.out.println("Failed to connect to mining database.");
            e.printStackTrace();
        }
    }

    public void BuildHTML() {
        if(miningDB == null) return;
        System.out.println("Building bad randoms HTML page...");

        try(BufferedWriter writer = new BufferedWriter(new FileWriter("badrandoms.html"))) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("<meta charset=\"UTF-8\">\n");
            writer.write("<title>Repeated randoms in the Stellar ledger</title>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            writer.write("<h1>Repeated randoms in the Stellar ledger</h1>\n");

            LocalDate dateTime = LocalDate.now();
            writer.write("<p> Generated on " + dateTime.toString() + ".</p>\n");

            int nBadRandoms = miningDB.getnBadRandoms();
            writer.write("<h3>" + nBadRandoms + " repeated randoms found</h3>\n");

            writer.write("<table cellspacing=\"10\" cellpadding=\"0\">\n");
            writer.write("<tbody>\n");

            // Add bad randoms to HTML page
            ResultSet rs = miningDB.getBadRandoms();
            while(rs.next()) {
                String random = rs.getString(1);
                BadRandom badRandom = miningDB.getBadRandomUses(random);
                Map<String, List<BadRandom.BadRandomContext>> usesGrouped = badRandom.groupUsesBySignerKey();

                // Create a HTML table of uses for this random.

                // Random, number of times used and date range of transactions (TODO)
                writer.write("<tr>\n");

                writer.write("<td valign=\"top\">\n");
                writer.write("<br>\n");
                writer.write(random);
                writer.write("</td>\n");

                writer.write("<td valign=\"top\">\n");
                writer.write("<br>\n");
                writer.write(badRandom.getnUses() + "x&nbsp;&nbsp;&nbsp;&nbsp;TODO:Date Range");
                writer.write("</td>\n");

                writer.write("</tr>\n");

                // Rows for nTransactions [AccountIDs] - SignerKey ............ [Transactions They Signed]
                writer.write("<tr>\n");

                for(Map.Entry<String, List<BadRandom.BadRandomContext>> entry : usesGrouped.entrySet()) {
                    String signerKey = entry.getKey();
                    List<BadRandom.BadRandomContext> brContexts = entry.getValue();
                    // A set of unique accounts that own this signer key. (should be 1).
                    Set<String> accountIDs = new HashSet<>();
                    for(BadRandom.BadRandomContext brContext : brContexts) {
                        accountIDs.add(brContext.getAccountID());
                    }

                    // [AccountIDs] - SignerKey
                    writer.write("<td valign=\"top\">\n");
                    writer.write("&nbsp;&nbsp;" + brContexts.size() + "x&nbsp;&nbsp;\n");
                    writer.write("[");
                    int i = 0;
                    for(String accountID : accountIDs) {
                        writer.write("<a href=\"https://horizon.stellar.org/accounts/" + accountID + "\">" + accountID +"</a>");
                        if(i < accountIDs.size() - 1) writer.write(",&nbsp;");
                        i++;
                    }
                    writer.write("]&nbsp;-&nbsp;" + signerKey + "\n");
                    writer.write("</td>\n");

                    // [Transactions They Signed]
                    writer.write("<td valign=\"top\">\n");
                    for(BadRandom.BadRandomContext brContext : brContexts) {
                        writer.write("<a href=\"https://horizon.stellar.org/transactions/" + brContext.getTxID() + "\">" + "tx" + brContext.getTxID().substring(0,4) + "</a>");
                        writer.write("&nbsp;");
                    }
                    writer.write("</td>\n");
                }

                writer.write("</tr>\n");
            }
            rs.close();

            writer.write("</tbody>\n");
            writer.write("</table>\n");
            writer.write("</body>\n");
            writer.write("</html>");

            System.out.println("Done");
        } catch (IOException e) {
            System.out.println("Failed to write to badrandoms.html");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error getting bad randoms from database.");
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
