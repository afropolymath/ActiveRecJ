/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afropolymath.db;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author chidieberennadi
 */
public class ActiveRecJ {
    private final Connection db;
    public static final int OVERWRITE_TRUE = 0;
    public static final int OVERWRITE_FALSE = 1;
    
    /**
     * ActiveRecJ Constructor
     * @param db Connection Object
     */
    public ActiveRecJ(Connection db) {
        this.db = db;
    }
    
    /**
     * 
     * @param driver
     * @param dsn
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException 
     */
    public static ActiveRecord createConnection(String driver, String dsn) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        Connection newConnection = DriverManager.getConnection(dsn);
        return new ActiveRecord(newConnection);
    }
    
    public Table createTable (String tblName, String[] fields, int overwrite) throws InvalidParameterException, SQLException {
        if(overwrite != OVERWRITE_TRUE && overwrite != OVERWRITE_FALSE)
            throw new InvalidParameterException("You have entered an invalid value for the overwrite parameter");

        if(fields.length <= 0)
            throw new InvalidParameterException("You have entered an invalid value for the fields parameter");

        try (Statement stmt = db.createStatement()) {
            StringBuilder q = new StringBuilder("create table ");
            if(overwrite == OVERWRITE_FALSE)
                q.append("if not exists ");
            q.append(tblName).append(" (");
            // Automatically creates the id field
            q.append("id integer PRIMARY KEY AUTOINCREMENT,");
            for (String field : fields) {
                q.append(field).append(",");
            }
            q.replace(-2, -1, ")");
            stmt.executeUpdate(q.toString());
            return new Table(tblName, fields);
        }
    }
    
    public ResultSet query(String q) throws SQLException {
        if(q != null) {
            try (Statement stmt = db.createStatement()) {
                return stmt.executeQuery(q);
            }
        }
        return null;
    }
    
    public class Table {
        
        private final String tblName;
        private final String[] fields;
        private StringBuilder qs;
        private final List<String> where;
        private int limit;
        private int index;
        
        public Table(String tblName, String[] fields) {
            this.tblName = tblName;
            this.fields = fields;
            this.where = new ArrayList<>();
        }
        
        public void insert(List values) throws SQLException, InvalidParameterException {
            if(values.size() <= 0)
                throw new InvalidParameterException("You have entered an invalid value for the values parameter");

            if(this.fields.length != values.size())
                throw new InvalidParameterException("The fields and the values parameter are expected to be the same size");

            try (Statement stmt = db.createStatement()) {
                String q = "insert into " + tblName + " (null, ";

                q += Arrays.toString(this.fields) + ")";

                q += " values(";
                Iterator it = values.iterator();
                while(it.hasNext()) {
                    Object val = it.next();
                    
                    if(it.next() instanceof String)
                        q += "'" + (String) val + "'";
                    else
                        q += val;
                    
                    q += it.hasNext() ? ", " : ")";
                }

                stmt.executeUpdate(q);
            }
        }
        
        public void update(int index, HashMap<String, ?> kv) throws SQLException, InvalidParameterException {
            if(index < 0) 
                throw new InvalidParameterException("The index parameter is expected to be an integer greater than 0");
            
            try (Statement stmt = db.createStatement()) {
                String q = "update " + tblName + " set ";
                Iterator it = kv.keySet().iterator();
                while(it.hasNext()){ 
                    String key = (String) it.next();
                    q += key + " = " + kv.get(key);
                    q += it.hasNext() ? ", " : " ";
                }
                q += "where id = " + index;
                stmt.executeUpdate(q);
            }
        }
        
        public Table find() {
            this.qs = new StringBuilder(String.format("select * from %s", this.tblName));
            return this;
        }
        public Table find(int index) {
            if(index < 0) 
                throw new InvalidParameterException("The index parameter is expected to be an integer greater than 0");
            this.index = index;
            return this;
        }
        
        public Table select(String[] fieldList) {
            return null;
        }
        public Table where(String condition) {
            if(this.where.size() > 0)
                condition = String.format("and %s", condition);
            
            this.where.add(condition);
            return this;
        }
        public Table or_where(String condition) {
            this.where.add(String.format("or %s", condition));
            return this;
        }
        public Table limit(int limit) {
            this.limit = limit;
            return this;
        } 
        public ResultSet exec() throws SQLException {
            // Build Query from qs and where clauses
            for(String c:this.where) {
                this.qs.append(" ").append(c);
            }
            this.qs.append("limit ").append(limit);
            try (Statement stmt = db.createStatement()) {
                return stmt.executeQuery(qs.toString());
            }
        }
    }
}
