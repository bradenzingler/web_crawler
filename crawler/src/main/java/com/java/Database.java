package com.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;


public class Database {
    private Connection conn;

    public Database() {
        try {

            // Init all of our database tables
            conn = DriverManager.getConnection("jdbc:sqlite:data_with_map.db");
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute(Statements.CREATE_URL_KEYWORDS_TABLE);
            stmt.execute(Statements.CREATE_KEYWORDS_TABLE);
            stmt.execute(Statements.CREATE_URL_TABLE);
            conn.commit();
        } catch (SQLException e) {
            System.out.println("Failed to connect to database: " + e.getMessage());
        }
    }


    /**
     * Sends a list of keywords to the keywords table in the database.
     * @param keywords the list of keywords to send.
     */
    private int sendKeyword(Keyword keyword, PreparedStatement keywordsStmt) throws SQLException {
        keywordsStmt.setString(1, keyword.toString());
        keywordsStmt.executeUpdate();

        try (ResultSet keys = keywordsStmt.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            } else {
                throw new SQLException("Creating URL failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    /**
     * Sends a URL and it's associated data to the urls table in the database.
     * @param url the url of the site to add
     * @param numKeywords the number of keywords for that site
     * @param description the metadata description for that site
     * @param title the title of that site
     * @return the url_id where the url was stored in the table.
     */
    private int sendUrl(String url, int numKeywords, String description, String title, PreparedStatement pstm) throws SQLException {
        pstm.setString(1, url);
        pstm.setInt(2, numKeywords);
        pstm.setString(3, description);
        pstm.setString(4, title);
        pstm.executeUpdate();

        try (ResultSet keys = pstm.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            } else {
                throw new SQLException("Creating URL failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Add the URL, keywords, and their associated tables to the database.
     * This is the main driver for the functionality behind adding to the database.
     * 
     * @param site the site being added to the database.
     */
    public void sendToDatabase(Site site) {

        try (PreparedStatement urlKeywordsStmt = conn.prepareStatement(Statements.INSERT_VALUES_URL_KEYWORDS);
            PreparedStatement keywordsStmt = conn.prepareStatement(Statements.INSERT_KEYWORDS, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement urlStmt = conn.prepareStatement(Statements.INSERT_URL, Statement.RETURN_GENERATED_KEYS)) {

            int urlId = sendUrl(site.getUrl(), site.getKeywords().size(), site.getDescription(), site.getTitle(), urlStmt);

            for (Map.Entry<Keyword, Double> entry : site.getKeywords().entrySet()) {

                // Send keyword if new one, get the keyword id
                Keyword keyword = entry.getKey();
                sendKeyword(keyword, keywordsStmt);
                int keywordId = getKeywordId(keyword.toString());

                // Calulate the term frequency for the keyword
                Double termFrequency = entry.getValue() / site.getKeywords().size();

                // Send all the keyword data to the url_keywords table
                urlKeywordsStmt.setInt(1, keywordId);
                urlKeywordsStmt.setInt(2, urlId);
                urlKeywordsStmt.setDouble(3, termFrequency);
                urlKeywordsStmt.addBatch();
               
            }

            urlKeywordsStmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Failed to rollback transaction: " + ex.getStackTrace());
            }
            System.out.println("Failed to write to database: " + e.getMessage());
        }
    }


    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Failed to close database connection: " + e.getMessage());
        }
    }


    /**
     * Get the total number of documents in the database.
     * 
     * @return the total number of documents
     */
    public int getNumDocs() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(Statements.TOTAL_NUM_URLS);
            return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Failed to get total number of documents: " + e.getMessage());
        }
        return 0;
    }

    
    /**
     * Get the keyword id for a given keyword.
     * 
     * @param urlId the id of the document
     * @return the total number of keywords in the document
     */
    private int getKeywordId(String keyword) {
        try (PreparedStatement pstm = conn.prepareStatement(Statements.GET_KEYWORD_ID)) {
            pstm.setString(1, keyword);
            ResultSet rs = pstm.executeQuery();
            return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Failed to get keyword id: " + e.getMessage());
            return -1;
        }
    }
}