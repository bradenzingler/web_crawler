package com.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


public class Database {
    private Connection conn;


    public Database() {
        conn = DriverManager.getConnection("jdbc:sqlite:data.db");
        conn.setAutoCommit(false);
    }


    /**
     * Add the URL, keywords, and embeddings to the database.
    * @param site the site being added to the database.
    */
    public void sendToDatabase(Site site) {
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try {
            Statement stmt = conn.createStatement();
            stmt.execute(Statements.CREATE_URL_KEYWORDS_TABLE);

            List<Double> embeddings = site.getEmbeddings();
            String title = site.getTitle();
            String description = site.getDescription();
            String url = site.getUrl();

            pstm = conn.prepareStatement(Statements.INSERT_VALUES);
            pstm.setString(1, embeddings.toString());
            pstm.setString(2, url);
            pstm.setString(3, title);
            pstm.setString(4, description);
            pstm.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Failed to rollback transaction: " + ex.getMessage());
            }
            System.out.println("Failed to write to database: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstm != null) pstm.close();
                if (this.conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Failed to close resources: " + e.getMessage());
            }
        }
    }
}
