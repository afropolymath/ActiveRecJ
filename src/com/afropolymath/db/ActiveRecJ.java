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
    /**
     * Connection object
     */
    private final Connection db;
    /**
     * Constant clears existing database tables 
     */
    public static final int OVERWRITE_TRUE = 0;
    /**
     * Constant retains existing database tables if they exist
     */
    public static final int OVERWRITE_FALSE = 1;
    
    /**
     * ActiveRecJ Constructor
     * 
     * @param db Connection Object
     */
    public ActiveRecJ(Connection db) {
        this.db = db;
    }
    
    /**
     * 
     * @param driver DB Connection driver
     * @param dsn Connection DSN string
     * @return ActiveRecJ object
     * @throws SQLException
     * @throws ClassNotFoundException 
     */
    public static ActiveRecJ createConnection(String driver, String dsn) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        Connection newConnection = DriverManager.getConnection(dsn);
        return new ActiveRecJ(newConnection);
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
        private List<String> s_fields;
        private StringBuilder qs;
        private final List<String> where;
        private int limit;
        private int index;
        
        public Table(String tblName, String[] fields) {
            this.tblName = tblName;
            this.fields = fields;
            this.where = new ArrayList<>();
            this.s_fields = new ArrayList<>();
        }
        
        /**
         * Inserts a list values into the database table
         * 
         * @param values List of values
         * @throws SQLException
         * @throws InvalidParameterException 
         */
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
        
        /**
         * Updates the fields represented in the kv HasMap Keyset with the values in the HashMap
         * 
         * @param index pk value
         * @param kv HashMap containing fields to update and their new values
         * @throws SQLException
         * @throws InvalidParameterException 
         */
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
        
        /**
         * Does an SQL select all statement
         * 
         * @return 
         */
        public Table find() {
            this.qs = new StringBuilder(String.format("select * from %s", this.tblName));
            return this;
        }
        
        /**
         * Create an SQL statement to select row of table with index <index>
         * 
         * @param index
         * @return 
         */
        public Table find(int index) {
            if(index < 0) 
                throw new InvalidParameterException("The index parameter is expected to be an integer greater than 0");
            this.index = index;
            return this;
        }
        
        /**
         * Does an SQL select of the fields in fieldList
         * 
         * @param fieldList List of columns to be selected
         * @return 
         */
        public Table select(String[] fieldList) {
            this.s_fields = Arrays.asList(fieldList);
            return this;
        }
        
        /**
         * Adds a condition to the search using the SQL WHERE
         * 
         * @param condition
         * @return 
         */
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
        
        /**
         * Adds a limit to the SQL query
         * 
         * @param limit
         * @return 
         */
        public Table limit(int limit) {
            this.limit = limit;
            return this;
        } 
        
        /**
         * Execute the stored SQL statement
         * 
         * @return
         * @throws SQLException 
         */
        public ResultSet exec() throws SQLException {
            if(this.s_fields.size() > 0) {
                this.qs = new StringBuilder("select ");
                for(String s:this.s_fields) {
                    this.qs.append(s).append(",");
                }
                this.qs.replace(-2, -1, String.format(" from %s", this.tblName));
            }
            
            if(this.where.size() > 0) {
                for(String c:this.where) {
                    this.qs.append(" ").append(c);
                }
            }
            this.qs.append("limit ").append(limit);
            try (Statement stmt = db.createStatement()) {
                return stmt.executeQuery(qs.toString());
            }
        }
    }
}
