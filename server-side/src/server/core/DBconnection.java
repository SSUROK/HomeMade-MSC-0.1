package server.core;

import java.sql.*;

public class DBconnection {

    //  Database credentials

    private static Connection connection = null;
    private static Statement statement;

    public DBconnection(String DB_URL, String USER, String PASS) {

        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement();
            System.out.println("You successfully connected to database now");
            createDbUserTable();
        } catch (SQLException e) {
            System.out.println("Connection Failed");
            e.printStackTrace();
        }
    }

    synchronized static void disconnect() {
        try {
            System.out.println("DB disconnected");
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createDbUserTable() throws SQLException {

        String createTableSQL = "CREATE TABLE IF NOT EXISTS connectedpcs("
                + "PC_ID serial PRIMARY KEY, "
                + "PC_NAME VARCHAR(40) NOT NULL, "
                + "OS VARCHAR(40) NOT NULL, "
                + "IP VARCHAR(40) NOT NULL, "
                + "RAMSPACE INTEGER NOT NULL, "
                + "DRIVES VARCHAR(40) NOT NULL, "
                + "RAMCRITICAL VARCHAR(40) "
                + ")";

        try {

            // выполнить SQL запрос
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void dbSave(String[] PC_SPECS) throws SQLException{
        try{
            ResultSet resultSet;
            String query = String.format("SELECT pc_name FROM connectedpcs WHERE pc_name='%s'", PC_SPECS[0]);
            resultSet = statement.executeQuery(query);
            if (resultSet.next()){
                query = String.format("UPDATE connectedpcs SET IP='%s', OS='%s', RAMSPACE='%s', DRIVES='%s' WHERE pc_name='%s'", PC_SPECS[1], PC_SPECS[2], PC_SPECS[3], PC_SPECS[4], PC_SPECS[0]);
                statement.executeUpdate(query);
                System.out.println("PC was successfully updated");
            } else {
                query = String.format("INSERT INTO connectedpcs(pc_name, ip, os, ramspace, drives) VALUES ('%s', '%s', '%s', '%s', '%s')", PC_SPECS);
                statement.executeUpdate(query);
                System.out.println("PC was successfully added");
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Save error");
            System.out.println(e.getMessage());
        }
    }

    public String dbGet(String pc_name) throws SQLException{
        String msg = "pc_specs;";
        try{
            ResultSet resultSet;
            String query = String.format("SELECT * FROM connectedpcs WHERE pc_name='%s'", pc_name);
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                msg = msg + resultSet.getString(2) + ";";
                msg = msg + resultSet.getString(3) + ";";
                msg = msg + resultSet.getString(4) + ";";
                msg = msg + resultSet.getString(5) + ";";
                msg = msg + resultSet.getString(6) + ";";
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Get error");
            System.out.println(e.getMessage());
        }
        return msg;
    }

    public String getPcsNames() throws SQLException{
        String msg = "pcs_names;";
        try{
            ResultSet resultSet;
            String query = String.format("SELECT pc_name FROM connectedpcs");
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                msg = msg + resultSet.getString(1) + ";";
            }

            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Get error");
            System.out.println(e.getMessage());
        }
        return msg;
    }

    public boolean test() throws SQLException{
        return statement.execute("SELECT * FROM connectedpcs");
    }
}
