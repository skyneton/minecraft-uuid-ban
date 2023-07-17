package net.mpoisv.ban;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseManager {
    private String path;
    private Connection connection;
    public DatabaseManager(String dir, String file) throws SQLException {
        var f = new File(dir, file);
        if(!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Can't create database file. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
            }
        }
        path = f.getAbsolutePath();
        if(path.startsWith("file:"))
            path = path.substring(5);
        path = "jdbc:sqlite:" + path;
        try {
            connection = DriverManager.getConnection(path);
        }catch(Exception e) {
            throw new RuntimeException("Can't create database connection. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
        }
        createTable();
    }

    public void close() throws SQLException {
        if(connection == null || connection.isClosed()) return;
        connection.close();
    }

    private void createTable() throws SQLException {
        var statement = connection.createStatement();
        statement.executeUpdate("create table if not exists uuid (uuid varchar(40) not null primary key, time INTEGER, reason not null);");
        statement.close();
    }

    public int insert(String uuid, long time, String reason) throws SQLException {
        var ps = connection.prepareStatement("insert into uuid values(?, ?, ?)");
        ps.setString(1, uuid);
        ps.setLong(2, time);
        ps.setString(3, reason);
        var result = ps.executeUpdate();
        ps.close();
        return result;
    }

    public int insert(String uuid, String reason) throws SQLException {
        var ps = connection.prepareStatement("insert into uuid(uuid, reason) values(?, ?)");
        ps.setString(1, uuid);
        ps.setString(2, reason);
        var result = ps.executeUpdate();
        ps.close();
        return result;
    }

    public void clean() throws SQLException {
        var now = System.currentTimeMillis();
        var ps = connection.prepareStatement("delete from uuid where time is not null and time < ?;");
        ps.setLong(1, now);
        ps.executeUpdate();
        ps.close();
    }

    public Data select(String uuid) throws SQLException {
        var ps = connection.prepareStatement("select * from uuid where uuid = ?;");
        ps.setString(1, uuid);
        var rs = ps.executeQuery();
        Data data = null;
        if(rs.next()) {
            data = new Data(rs.getString("uuid"), rs.getLong("time"), rs.getString("reason"));
        }
        rs.close();
        ps.close();
        return data;
    }

    public int delete(String uuid) throws SQLException {
        var ps = connection.prepareStatement("delete from uuid where uuid = ?;");
        ps.setString(1, uuid);
        var result = ps.executeUpdate();
        ps.close();
        return result;
    }

    public int update(String uuid, long time, String reason) throws SQLException {
        var ps = connection.prepareStatement("update uuid set time = ?, reason = ? where uuid = ?");
        ps.setLong(1, time);
        ps.setString(2, reason);
        ps.setString(3, uuid);
        var result = ps.executeUpdate();
        ps.close();
        return result;
    }

    public int update(String uuid, String reason) throws SQLException {
        var ps = connection.prepareStatement("update uuid set time = NULL, reason = ? where uuid = ?");
        ps.setString(1, reason);
        ps.setString(2, uuid);
        var result = ps.executeUpdate();
        ps.close();
        return result;
    }

    public ArrayList<Data> getBanUserList() throws SQLException {
        var statement = connection.createStatement();
        var rs = statement.executeQuery("select * from uuid;");
        var list = new ArrayList<Data>();
        while(rs.next()) {
            list.add(new Data(
                    rs.getString("uuid"),
                    rs.getLong("time"),
                    rs.getString("reason")));
        }
        rs.close();
        return list;
    }

    public Pagination getPagination(int page, int pageCount) throws SQLException {
        int count = getCount();
        var ps = connection.prepareStatement("select * from uuid limit ? offset ?;");
        ps.setLong(1, pageCount);
        ps.setLong(2, (long) (page - 1) * pageCount);
        var rs = ps.executeQuery();
        var list = new ArrayList<Data>();
        while(rs.next()) {
            list.add(new Data(
                    rs.getString("uuid"),
                    rs.getLong("time"),
                    rs.getString("reason")));
        }
        rs.close();
        return new Pagination(page, (int)Math.ceil((double)count / pageCount), list);
    }

    public int getCount() throws SQLException {
        var statement = connection.createStatement();
        var rs = statement.executeQuery("select count(uuid) from uuid");
        int count = 0;
        if(rs.next())
            count = rs.getInt(1);
        rs.close();
        return count;
    }

    public class Data {
        private String uuid;
        private long time;
        private String reason;
        public Data(String uuid, long time, String reason) {
            this.uuid = uuid;
            this.time = time;
            this.reason = reason;
        }

        public String getUuid() {
            return uuid;
        }

        public long getTime() {
            return time;
        }

        public String getReason() {
            return reason;
        }
    }

    public class Pagination {
        private int currentPage;
        private int maxPage;
        private ArrayList<Data>values;
        public Pagination(int currentPage, int maxPage, ArrayList<Data> values) {
            this.currentPage = currentPage;
            this.maxPage = maxPage;
            this.values = values;
        }

        public ArrayList<Data> getValues() {
            return values;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getMaxPage() {
            return maxPage;
        }
    }
}
