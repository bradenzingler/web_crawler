/**
 * A simple web crawler that extracts keywords from a website.
 * The keywords are then lemmatized and output to a relational database,
 * along with the urls.
 * Braden Zingler
 */

package com.java;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.panforge.robotstxt.RobotsTxt;        //https://github.com/pandzel/RobotsTxt
import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Crawler {

    public static void main(String[] args) {

        
        try {
            // Set up the dataset of links to visit
            File urlsToVisit = new File("crawler/src/main/resources/unvisited.csv");
            Scanner scnr = new Scanner(urlsToVisit);
            FileWriter writer = new FileWriter(urlsToVisit, true);

            // Read in the lemmatization values
            Lemmatizer lem = new Lemmatizer();

            // Set up tracking for already visited links
            ArrayList<String> visited = new ArrayList<>();
            
            // Iterate through the links to visit
            while (scnr.hasNextLine()) {
                String stringUrl = scnr.nextLine().split(",")[1].replace("\"", "").strip();
                try {
                    if (!visited.contains(stringUrl)){
                        visited.add(stringUrl);
                        String url = stringUrl.contains("https") ? stringUrl :  "https://" + stringUrl;
                        
                        // Check robots.txt
                        if (!isAllowedByRobotsTxt(url)) continue;
                        Document doc = Jsoup.connect(url).get();
                        doc.outputSettings().charset("UTF-8");

                        // Get keywords
                        String text = doc.select("p, h1, h2, h3, h4, h5, h6, title").text();
                        String[] words = text.split(" ");
                        List<String> keywords = filterKeywords(words, lem);

                        // Output information to the database
                        sendToDatabase(url, keywords);

                        System.out.println("Scraped " + url);
                    }
                } catch(Exception e) {
                    System.out.println("Error: " + stringUrl);
                }
            }
                writer.close();
                scnr.close();
        }  catch(Exception e) {
            System.out.println("Failed to read/write file: "+e);
        }
    }


    /**
     * Sends a url and it's keywords to the database.
     */
    public static void sendToDatabase(String url, List<String> keywords) {
        PreparedStatement pstm = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:wikipedia.db");
            Statement stmt = conn.createStatement();
            conn.setAutoCommit(false);

            // Create the urls table
            String createUrlTable = "CREATE TABLE IF NOT EXISTS urls ("
                                    + "url_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "url TEXT UNIQUE)";
            stmt.execute(createUrlTable);

            // Create the keywords table
            String createKeywordsTable = "CREATE TABLE IF NOT EXISTS keywords (keyword_id INTEGER PRIMARY KEY AUTOINCREMENT, keyword TEXT UNIQUE)";
            stmt.execute(createKeywordsTable);

            // Create the url-keywords table
            String createAssociationTable = "CREATE TABLE IF NOT EXISTS url_keywords("
                                            + "url_id INTEGER, "
                                            + "keyword_id INTEGER, "
                                            + "PRIMARY KEY (url_id, keyword_id), "
                                            + "UNIQUE (url_id, keyword_id), "
                                            + "FOREIGN KEY (url_id) REFERENCES urls(url_id), "
                                            + "FOREIGN KEY (keyword_id) REFERENCES keywords(keyword_id))";
            stmt.execute(createAssociationTable);

            // Add url to urls table and save the ids
            String addUrl = "INSERT OR IGNORE INTO urls (url) VALUES (?)";
            pstm = conn.prepareStatement(addUrl, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, url);
            pstm.executeUpdate();
            rs = pstm.getGeneratedKeys();
            int urlId;
            if (rs.next()) {
                urlId = rs.getInt(1);
            } else {
                // If the URL already exists, fetch the existing url_id
                String selectUrlIdSQL = "SELECT url_id FROM urls WHERE url = ?";
                pstm = conn.prepareStatement(selectUrlIdSQL);
                pstm.setString(1, url);
                rs = pstm.executeQuery();
                if (rs.next()) {
                    urlId = rs.getInt("url_id");
                } else {
                    throw new SQLException("Failed to retrieve URL ID");
                }
            }

            // Insert each keyword and get keyword ids
            String insertKeyword = "INSERT OR IGNORE INTO keywords (keyword) VALUES (?)";
            String selectKeywordId = "SELECT keyword_id FROM keywords WHERE keyword = ?";
            for (String keyword : keywords) {
                pstm = conn.prepareStatement(insertKeyword);
                pstm.setString(1, keyword);
                pstm.executeUpdate();

                pstm = conn.prepareStatement(selectKeywordId);
                pstm.setString(1, keyword);
                rs = pstm.executeQuery();
                int keywordId;
                if (rs.next()) {
                    keywordId = rs.getInt("keyword_id");
                } else {
                    throw new SQLException("Failed to retrieve keyword ID");
                }

                // Step 3: Insert URL-Keyword association
                String insertUrlKeywordSQL = "INSERT OR IGNORE INTO url_keywords (url_id, keyword_id) VALUES (?, ?)";
                pstm = conn.prepareStatement(insertUrlKeywordSQL);
                pstm.setInt(1, urlId);
                pstm.setInt(2, keywordId);
                pstm.executeUpdate();
            }
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
            // close all resources
            try {
                if (rs != null) rs.close();
                if (pstm != null) pstm.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Failed to close resources: " + e.getMessage());
            }
        }
    }


    /**
     * Checks that a website allows web scraping.
     * @param targetUrl the url to check.
     * @return true if the website allows web scraping, false otherwise.
     */
    private static boolean isAllowedByRobotsTxt(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            String host = url.getProtocol() + "://" + url.getHost();

            URL robotsUrl = new URL(host + "/robots.txt");
            HttpURLConnection connection = (HttpURLConnection) robotsUrl.openConnection();
            connection.setReadTimeout(2000);
            RobotsTxt robotsTxt = RobotsTxt.read(connection.getInputStream());

            return robotsTxt.query("scraper", targetUrl);
        } catch (Exception e) {
            System.out.println(targetUrl + " disallowed scraping.");
        }
        return false;
    }


    /**
     * Filter out the stopwords from the list of words
     * @param words The list of words
     * @return The list of words without the stopwords
     */
    private static List<String> filterKeywords(String[] words, Lemmatizer lem) {
        try {
            List<String> filteredKeywords = new ArrayList<>();
            for (String word : words) {
                if (!isStopword(word) && isWord(word)) {
                    String newWord = lem.lemmatizeWord(word.toLowerCase());
                    filteredKeywords.add(newWord);
                }
            }
            return filteredKeywords;
        } catch (Exception e) {
            System.out.println("Error while filtering keywords: " + e);
        }
        return null;
    }


    /**
     * Checks if the current word is a word and does not contain special characters.
     * @param word the word to check.
     * @return true if doesn't contain the ignored characters, false otherwise.
     */
    private static boolean isWord(String word) {
        try {
            return word.matches("\\b\\w+\\b");
        } catch (Exception e) {
            System.out.println("An error occurred while checking if " + word + " is a word: " + e);
        }
        return false;
    }


    /**
     * Check if a word is a stopword
     * @param word The word to check
     * @return True if the word is a stopword, false otherwise
     */
    private static boolean isStopword(String word) {
        try {
            String[] stopWords = new String[] {"a","about","above","according","across","actually","after","again","against","all","almost","also","although","always","am","among","amongst","an","and","any","anything","anyway","are","as","at","be","became","become","because","been","before","being","below","between","both","but","by","can","could","did","do","does","doing","down","during","each","either","else","few","for","from","further","had","has","have","having","he","he'd","he'll","hence","he's","her","here","here's","hers","herself","him","himself","his","how","how's","I","I'd","I'll","I'm","I've","if","in","into","is","it","it's","its","itself","just","let's","may","maybe","me","might","mine","more","most","must","my","myself","neither","nor","not","of","oh","on","once","only","ok","or","other","ought","our","ours","ourselves","out","over","own","same","she","she'd","she'll","she's","should","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there","there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","we","we'd","we'll","we're","we've","were","what","what's","when","whenever","when's","where","whereas","wherever","where's","whether","which","while","who","whoever","who's","whose","whom","why","why's","will","with","within","would","yes","yet","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"};
            for (String stopWord : stopWords) {
                if (word.equalsIgnoreCase(stopWord)) return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("An error occurred while checking if is stopword: " + e);
        }
        return true;
    }
}