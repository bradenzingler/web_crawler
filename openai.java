/**
 * A simple web crawler that extracts keywords from a website.
 * The keywords are then lemmatized and output to a relational database,
 * along with the urls.
 * Braden Zingler
 */

package com.java;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panforge.robotstxt.RobotsTxt;        //https://github.com/pandzel/RobotsTxt
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.theokanning.openai.embedding.*;
import com.theokanning.openai.service.OpenAiService;

import java.sql.Blob;
import java.sql.Blob.*;

public class Crawler {

    private static final List<String> filters = List.of(
    "https://en.wikipedia.org/wiki/Help:Category",
    "https://en.wikipedia.org/wiki/Category",
    "https://en.wikipedia.org/wiki/Template",
    "https://en.wikipedia.org/wiki/Portal",
    "https://en.wikipedia.org/wiki/Special",
    "https://en.wikipedia.org/wiki/Wikipedia",
    "https://en.wikipedia.org/wiki/File",
    "https://en.wikipedia.org/wiki/Module",
    "https://en.wikipedia.org/wiki/Book",
    "https://en.wikipedia.org/wiki/Help",
    "https://en.wikipedia.org/wiki/Talk"
);

    public static void main(String[] args) {
        try {
            // Set up the dataset of links to visit
            File urlsToVisit = new File("crawler/src/main/resources/small.csv");
            Scanner scnr = new Scanner(urlsToVisit);
            FileWriter writer = new FileWriter(urlsToVisit, true);

            // Read in the lemmatization values
            Lemmatizer lem = new Lemmatizer();

            // Set up tracking for already visited links
            ArrayList<String> visited = new ArrayList<>();
            
            // Iterate through the links to visit
            while (scnr.hasNextLine()) {
                String stringUrl = scnr.nextLine();
                try {
                    if (!visited.contains(stringUrl) && stringUrl.startsWith("https://en.wikipedia.org/wiki")
                    && !stringUrl.contains("Help:") && !stringUrl.contains("File:") && !stringUrl.contains("Category:")
                    && !stringUrl.contains("Template:") && !stringUrl.contains("Portal:") && !stringUrl.contains("Special:")
                    && !stringUrl.contains("Module:") && !stringUrl.contains("Book:")){

                        visited.add(stringUrl);
                        String url = stringUrl.contains("https") ? stringUrl :  "https://" + stringUrl;
                        
                        // Check robots.txt
                        //if (!isAllowedByRobotsTxt(url)) continue;
                        long startTime = System.currentTimeMillis();
                        Document doc = Jsoup.connect(url).get();
                        doc.outputSettings().charset("UTF-8");
                        long endTime = System.currentTimeMillis();
                        System.out.println("Time to fetch page: " + (endTime - startTime) + " ms");


                        // Get revelant information from the website 
                        String text = doc.select("p, h1, h2, h3, h4, h5, h6, title").text();
                        String[] words = text.split(" ");
                        String title = doc.title();
                        String desc = doc.getElementsByClass("shortdescription").text();
                        long tp = System.currentTimeMillis();
                        System.out.println("Time to parse text: " + (tp - endTime) + " ms");
                        List<String> keywords = filterKeywords(words, lem);
                        long tfk = System.currentTimeMillis();
                        System.out.println("Time to filter keywords: " + (tfk - tp) + " ms");
                        HashMap<String, Double> tf = getTf(keywords); // get term frequencies to choose best keywords
                        long endTime2 = System.currentTimeMillis();
                        System.out.println("Time to get tf: " + (endTime2 - tfk) + " ms");
                        List<String> newKeywords = new ArrayList<String>(tf.keySet());
                        HashMap<String, List<Double>> embeds = getTextEmbeddings(newKeywords);
                        long startTime3 = System.currentTimeMillis();
                        System.out.println("Time to parse all keywords: " + (startTime3 - endTime) + " ms");

                        // Output information to the database
                        sendToDatabase(url, newKeywords, title, desc, embeds);
                        long endTime3 = System.currentTimeMillis();
                        System.out.println("Time to send to database: " + (endTime3 - startTime3) + " ms");

                        // Write discovered urls to the file
                        List<String> discoveredUrls = doc.select("a").eachAttr("href");
                        discoveredUrls = discoveredUrls.stream().limit(1000).collect(Collectors.toList());
                        for (String discoveredUrl : discoveredUrls) {
                            String discoURL = discoveredUrl.contains("https") ? discoveredUrl : "https://en.wikipedia.org" + discoveredUrl;
                            if (!visited.contains(discoveredUrl) && !filters.stream().anyMatch(discoURL::contains)) {
                                writer.write(discoURL + "\n");
                            }
                        }
                        writer.flush();
                        long endTime23 = System.currentTimeMillis();
                        System.out.println("Time to write to file: " + (endTime23 - endTime3) + " ms");

                        System.out.println("Scraped " + url);
                    }
                } catch(Exception e) {
                    System.out.println("Error: " + e);
                    e.printStackTrace();
                }
            }
                writer.close();
                scnr.close();
        }  catch(Exception e) {
            System.out.println("Failed to read/write file: "+e);
        }
    }


    /**
     * Get the embeddings for the keywords using OpenAI's text-embedding-3-small model
     * @param keywords The keywords to get embeddings for
     * @return A map of keywords to their embedding vectors.
     */
    private static HashMap<String, List<Double>> getTextEmbeddings(List<String> keywords) {
        long t0 = System.currentTimeMillis();
        HashMap<String, List<Double>> embeds = new HashMap<>();
        try {
            OpenAiService service = new OpenAiService();
            EmbeddingRequest request = EmbeddingRequest.builder().input(keywords).model("text-embedding-3-small").build();
            EmbeddingResult result = service.createEmbeddings(request);
            System.out.println(request);
            List<Embedding> resultList = result.getData();
            
            for (int i = 0; i < keywords.size(); i++) {
                Embedding e = resultList.get(i);
                List<Double> batch = e.getEmbedding();
                embeds.put(keywords.get(i), batch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Time to get embeddings: " + (t1 - t0) + " ms");
        return embeds;
    }


    /**
     * Convert a list of embeddings to a string to be stored in the database.
     * @param embeds The list of embeddings
     * @return The string representation of the embeddings
     */
    public static String convertToString(List<Double> embeds) {
       ObjectMapper mapper = new ObjectMapper();
       try {
              String embedsJson = mapper.writeValueAsString(embeds);
              return embedsJson;
         } catch (Exception e) {
              System.out.println("Failed to convert embeddings to string: " + e);
       }
       return null;
    }


    /**
     * Send the URL and keywords to the database
     * @param url The URL to send
     * @param keywords The keywords to send
     * @throws SQLException If there is an error writing to the database
     * @throws ClassNotFoundException If the SQLite driver is not found
     * @throws Exception If there is an error closing the resources
     */
    public static void sendToDatabase(String url, List<String> keywords, String title, String description, HashMap<String, List<Double>> embedMap) {
        PreparedStatement pstm = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:data2.db");
            Statement stmt = conn.createStatement();
            conn.setAutoCommit(false);

            // Create the urls table
            String createUrlTable = "CREATE TABLE IF NOT EXISTS urls ("
                                    + "url_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "url TEXT UNIQUE, "
                                    + "num_keywords INTEGER, " 
                                    + "title TEXT, "
                                    + "description TEXT)";
            stmt.execute(createUrlTable);

            // Create the keywords table
            String createKeywordsTable = "CREATE TABLE IF NOT EXISTS keywords (keyword_id INTEGER PRIMARY KEY AUTOINCREMENT, keyword TEXT UNIQUE)";
            stmt.execute(createKeywordsTable);

            // Create the url-keywords table
            String createAssociationTable = "CREATE TABLE IF NOT EXISTS url_keywords("
                                            + "url_id INTEGER, "
                                            + "num_occurences INTEGER, "
                                            + "keyword_id INTEGER, "
                                            + "embedding TEXT, "
                                            + "PRIMARY KEY (url_id, keyword_id), "
                                            + "UNIQUE (url_id, keyword_id), "
                                            + "FOREIGN KEY (url_id) REFERENCES urls(url_id), "
                                            + "FOREIGN KEY (keyword_id) REFERENCES keywords(keyword_id))";
            stmt.execute(createAssociationTable);

            // Add url to urls table and save the ids
            String addUrl = "INSERT OR IGNORE INTO urls (url, num_keywords, title, description) VALUES (?, ?, ?, ?)";
            pstm = conn.prepareStatement(addUrl, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, url);
            pstm.setInt(2, keywords.size());
            pstm.setString(3, title);
            pstm.setString(4, description);
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
                List<Double> embeds = embedMap.get(keyword);

                if (embeds == null) {
                    System.out.println("Failed to get embeddings for keyword: " + keyword);
                    continue;
                }
                String jsonEmbeds = convertToString(embeds);
                
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

                // Insert URL-Keyword association
                String insertUrlKeywordSQL = "INSERT OR IGNORE INTO url_keywords (url_id, keyword_id, num_occurences, embedding) VALUES (?, ?, ?, ?)";
                pstm = conn.prepareStatement(insertUrlKeywordSQL);
                pstm.setInt(1, urlId);
                pstm.setInt(2, keywordId);
                pstm.setInt(3, keywords.size()); // number of occurences
                pstm.setString(4, jsonEmbeds);
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
            e.printStackTrace();
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


    private static HashMap<String, Double> getTf(List<String> words) {
        HashMap<String, Double> tf = new HashMap<>();
        for (String word : words) {
            tf.put(word, tf.getOrDefault(word, 0.0) + 1.0);
        }
        for (String word : tf.keySet()) {
            tf.put(word, tf.get(word) / words.size());
        }
         
        // only keep the top 30 keywords
       HashMap<String, Double> top20Tf = tf.entrySet().stream()
            .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
            .limit(30)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));
        return top20Tf;
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
                if (isWord(word)) {
                    if (!isStopword(word)) {
                        filteredKeywords.add(word.toLowerCase());
                    }
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
            boolean isWord = word.matches("\\b\\w+\\b");
            return isWord;
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
        boolean result = false;
        try {
            String[] stopWords = new String[] {"a","about","above","according","across","actually","after","again","against","all","almost","also","although","always","am","among","amongst","an","and","any","anything","anyway","are","as","at","be","became","become","because","been","before","being","below","between","both","but","by","can","could","did","do","does","doing","down","during","each","either","else","few","for","from","further","had","has","have","having","he","he'd","he'll","hence","he's","her","here","here's","hers","herself","him","himself","his","how","how's","I","I'd","I'll","I'm","I've","if","in","into","is","it","it's","its","itself","just","let's","may","maybe","me","might","mine","more","most","must","my","myself","neither","nor","not","of","oh","on","once","only","ok","or","other","ought","our","ours","ourselves","out","over","own","same","she","she'd","she'll","she's","should","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there","there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","we","we'd","we'll","we're","we've","were","what","what's","when","whenever","when's","where","whereas","wherever","where's","whether","which","while","who","whoever","who's","whose","whom","why","why's","will","with","within","would","yes","yet","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"};
            for (String stopWord : stopWords) {
                if (word.equalsIgnoreCase(stopWord)) result = true;
            }
        } catch (Exception e) {
            System.out.println("An error occurred while checking if is stopword: " + e);
        }
        return result;
    }
}
