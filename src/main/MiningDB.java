package main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MiningDB {
    private Connection conn;

    public MiningDB(String url, String username, String password) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        conn = DriverManager.getConnection(url, username, password);
    }

    public void createTableEd25519() {
        
    }

    public void close() throws SQLException {
        conn.close();
    }
}

