package com.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private Connection conn;

    public Database() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:data.db");
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute(Statements.CREATE_URL_KEYWORDS_TABLE);
        } catch (SQLException e) {
            System.out.println("Failed to connect to database: " + e.getMessage());
        }
    }

    /**
     * Add the URL, keywords, and embeddings to the database.
     * 
     * @param site the site being added to the database.
     */
    public void sendToDatabase(Site site) {
        try (PreparedStatement pstm = conn.prepareStatement(Statements.INSERT_VALUES)) {
            String title = site.getTitle();
            String description = site.getDescription();
            String url = site.getUrl();

            for (Keyword keyword : site.getKeywords()) {
                pstm.setString(1, keyword.toString());
                pstm.setDouble(2, site.getTermFrequency(keyword));
                pstm.setString(3, url);
                pstm.setString(4, title);
                pstm.setString(5, description);
                pstm.executeUpdate();
            }

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
            ResultSet rs = stmt.executeQuery(Statements.TOTAL_NUM_DOCS);
            return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Failed to get total number of documents: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Calculate the IDF (Inverse Document Frequency) for a given term.
     * 
     * @param term the term for which IDF is calculated
     * @return the IDF value
     */
    public double calculateIdf(String term) {
        try {
            PreparedStatement pstm = conn.prepareStatement(Statements.GET_NUM_KEYWORDS);
            pstm.setString(1, term);
            ResultSet rs = pstm.executeQuery();
            int numTerms = rs.getInt(1);
            double idf = Math.log(1 + (getNumDocs() / (double) numTerms));
            return idf;
        } catch (SQLException e) {
            System.out.println("Failed to calculate IDF: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Compute TF-IDF values and store them in the database.
     */
    public void computeTfIdf() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM url_keywords");
            while (rs.next()) {
                String keyword = rs.getString("keyword");
                double tf = rs.getDouble("term_frequency");
                double idf = calculateIdf(keyword); // Calculate IDF for the keyword
                double tfidf = tf * idf;

                // Update the tfidf column for the current keyword
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE url_keywords SET tfidf = ? WHERE keyword = ?");
                updateStmt.setDouble(1, tfidf);
                updateStmt.setString(2, keyword);
                updateStmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            System.out.println("Failed to compute TF-IDF: " + e.getMessage());
        }
    }
}
