package server.core;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBconnection {

    //  Database credentials

    private static Connection connection = null;
    private static Statement statement;

    public DBconnection(String DB_URL, String USER, String PASS, Server server) {

        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement();
            System.out.println("You successfully connected to database now");
            server.succConnectDB();
            createDbUserTable();
        } catch (SQLException e) {
            System.out.println("Connection Failed");
            server.errorConnectDB();
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

        String createPCLIST = "CREATE TABLE IF NOT EXISTS computer("
                + "PC_ID serial PRIMARY KEY, "
                + "PC_NAME VARCHAR(40) NOT NULL UNIQUE, "
                + "OS VARCHAR(40) NOT NULL, "
                + "IP VARCHAR(40) NOT NULL UNIQUE, "
                + "RAMSPACE INTEGER NOT NULL, "
                + "RAMCRITICAL VARCHAR(40), "
                + "online BOOLEAN, "
                + "exit_time TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP "
                + ")";

        String createDRIVELIST = "CREATE TABLE IF NOT EXISTS drive ("
        + "ID SERIAL PRIMARY KEY, "
                + "pc_name VARCHAR(40) NOT NULL REFERENCES computer (pc_name), "
        + "name VARCHAR(40) NOT NULL, "
        + "space INTEGER NOT NULL, "
        + "critical_space INTEGER NOT NULL"
        + ")";

        try {

            // выполнить SQL запрос
            statement.execute(createPCLIST);

            statement.execute(createDRIVELIST);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void dbSave(String[] PC_SPECS) throws SQLException {
        try {
            ResultSet resultSet;
            String query = String.format("SELECT ip FROM computer WHERE ip='%s'", PC_SPECS[1]);
            resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                query = String.format("UPDATE computer SET IP='%s', OS='%s', RAMSPACE='%s' WHERE pc_name='%s'", PC_SPECS[1], PC_SPECS[2], PC_SPECS[3], PC_SPECS[0]);
                statement.executeUpdate(query);
                System.out.println("PC was successfully updated");

                String[] drives =  PC_SPECS[4].split(" ");
                for (String drive : drives) {
                    String[] drive_ =  drive.split("=");
                    query = String.format("UPDATE drive SET space='%s' WHERE pc_name='%s' AND name='%s'", drive_[1], PC_SPECS[0], drive_[0]);
                    statement.executeUpdate(query);
                    System.out.println("DRIVES was successfully updated");
                }

            } else {
                query = String.format("INSERT INTO computer(pc_name, ip, os, ramspace) VALUES ('%s', '%s', '%s', '%s')", PC_SPECS[0], PC_SPECS[1], PC_SPECS[2], PC_SPECS[3]);
                statement.executeUpdate(query);
                setOnline(PC_SPECS[1]);
                System.out.println("PC was successfully added");

                String[] drives =  PC_SPECS[4].split(" ");
                for (String drive : drives) {
                    String[] drive_ =  drive.split("=");
                    query = String.format("INSERT INTO drive (pc_name, name, space, critical_space) VALUES ('%s', '%s', '%s', 1)", PC_SPECS[0], drive_[0], drive_[1]);
                    statement.executeUpdate(query);
                    System.out.println("DRIVES was successfully added");
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Save error");
            System.out.println(e.getMessage());
        }
    }

    public Map<String, Map<String, List<Integer>>> getDrives() throws SQLException{
        Map<String, Map<String, List<Integer>>> drives = new HashMap<>();
        try {
            ResultSet resultSet;
            String query = String.format("SELECT * FROM drive");
            resultSet = statement.executeQuery(query);
            while (resultSet.next()){
                List<Integer> space = new ArrayList<>();
                Map<String, List<Integer>> drive = new HashMap<>();

                space.add(Integer.parseInt(resultSet.getString("space")));
                space.add(Integer.parseInt(resultSet.getString("critical_space")));
                drive.put(resultSet.getString("name"), space);

                if(drives.containsKey(resultSet.getString("pc_name"))){
                    drives.get(resultSet.getString("pc_name")).put(resultSet.getString("name"), space);
                }else{

                    drives.put(resultSet.getString("pc_name"), drive);
                }
            }
        } catch (SQLException e) {
            System.out.println("Get error");
            System.out.println(e.getMessage());
        }
        return drives;
    }

    public Map<String, List<Integer>> getRam() throws SQLException{
        Map<String, List<Integer>> ram = new HashMap<>();
        try {
            ResultSet resultSet;
            String query = String.format("SELECT pc_name, ramspace, ramcritical FROM computer");
            resultSet = statement.executeQuery(query);
            while (resultSet.next()){
                List<Integer> space = new ArrayList<>();

                space.add(Integer.parseInt(resultSet.getString("ramspace")));
                space.add(Integer.parseInt(resultSet.getString("ramcritical")));
                ram.put(resultSet.getString("pc_name"), space);
            }
        } catch (SQLException e) {
            System.out.println("Get error");
            System.out.println(e.getMessage());
        }
        return ram;
    }

    public String dbGet(String pc_name) throws SQLException {
        String msg = "pc_specs;";
        try {
            ResultSet resultSet;
            String query = String.format("SELECT * FROM computer WHERE pc_name='%s'", pc_name);
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                msg = msg + resultSet.getString(2) + ";";
                msg = msg + resultSet.getString(3) + ";";
                msg = msg + resultSet.getString(4) + ";";
                msg = msg + resultSet.getString(5) + ";";
                msg = msg + resultSet.getString(6) + ";";
                if (resultSet.getString(7).equals("f")){
                    msg = msg + resultSet.getString(8);
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Get error");
            System.out.println(e.getMessage());
        }
        return msg;
    }

    public String getPcsNames(){
        String msg = "pcs_names;";
        try {
            ResultSet resultSet;
            String query = String.format("SELECT pc_name FROM computer");
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

    public boolean test() throws SQLException {
        return statement.execute("SELECT * FROM computer");
    }

    public boolean check(String ip) throws SQLException {
        String query = String.format("SELECT * FROM computer WHERE ip='%s' AND online='true'", ip);
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet.next();
    }

    public void setOnline(String ip) throws SQLException {
        String query = String.format("UPDATE computer SET online='true' WHERE ip='%s'", ip);
        statement.executeUpdate(query);
    }

    public void setOffline(String ip) throws SQLException {
        String query = String.format("UPDATE computer SET online='false' WHERE ip='%s'", ip);
        statement.executeUpdate(query);
        query = String.format("UPDATE computer SET exit_time=CURRENT_TIMESTAMP WHERE ip='%s'", ip);
        statement.executeUpdate(query);
    }

    public void setLimit(String msg) throws SQLException {
        if (msg.contains("ram")) {
            msg = msg.substring(3);
            String[] m = msg.split(";");
            String query = String.format("UPDATE computer SET ramcritical='%s' WHERE pc_name='%s'", m[1], m[0]);
            statement.executeUpdate(query);
        } else{
            String[] m = msg.split(";");
            String query = String.format("UPDATE drive SET critical_space='%s' WHERE pc_name='%s' AND name='%s'", m[2], m[0], m[1]);
            statement.executeUpdate(query);
        }

    }
}
