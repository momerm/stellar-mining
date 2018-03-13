package main;

import javafx.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BadRandomMiner {
    private MiningDB miningDB = null;

    public BadRandomMiner(String url, String username, String password) {
        try {
            miningDB = new MiningDB(url + "mining", username, password);
        }
        catch (SQLException e) {
            miningDB = null;
            System.out.println("Failed to connect to mining database.");
            e.printStackTrace();
        }
    }

    public void mineBadRandoms() {
        if(miningDB == null) return;
        System.out.println("Finding bad randoms...");
        // Create tables in mining database
        try {
            miningDB.dropTableBadRandom();
            miningDB.createTableBadRandom();
        }
        catch (SQLException e) {
            System.out.println("Failed to create tables in mining database.");
            e.printStackTrace();
            return;
        }

        // Find repeated randoms
        List<Pair<String, Integer>> badRandoms = new ArrayList<>(100);
        int n = 0; // Number of bad randoms found.
        try {
            ResultSet badRandomResultSet = miningDB.findBadRandoms();
            while (badRandomResultSet.next()) {
                String random = badRandomResultSet.getString(1);
                int nUses = badRandomResultSet.getInt(2);

                badRandoms.add(new Pair<>(random, nUses));
                n++;

                if(badRandoms.size() == 100) {
                    miningDB.insertBadRandoms(badRandoms);
                    badRandoms.clear();
                }
            }
            badRandomResultSet.close();

            if(badRandoms.size() > 0)
                miningDB.insertBadRandoms(badRandoms);

            System.out.printf("Found %d bad randoms.\n", n);
        } catch (Exception e) {
            System.out.println("Error finding bad randoms.");
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