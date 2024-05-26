package com.java;

public class Statements {

    /* url_keywords table operations */
    public static final String CREATE_URL_KEYWORDS_TABLE = "CREATE TABLE IF NOT EXISTS url_keywords("
    + "keyword_id INTEGER, "
    + "url_id INTEGER, "
    + "term_frequency REAL, "
    + "tfidf REAL)";                    
    public static final String INSERT_VALUES_URL_KEYWORDS = "INSERT OR IGNORE INTO url_keywords(keyword_id, url_id, term_frequency) VALUES (?, ?, ?)";
    public static final String GET_NUM_KEYWORD_OCCURRENCE = "SELECT num_occurrences FROM url_keywords WHERE keyword_id = ? AND url_id = ?";
    public static final String GET_TOTAL_NUM_URLS_WITH_KEYWORD = "SELECT COUNT(url_id) FROM url_keywords WHERE keyword_id = ?";
    public static final String SET_TFIDF = "UPDATE url_keywords SET tfidf = ? WHERE keyword_id = ? AND url_id = ?";
    public static final String SET_IDF = "UPDATE keywords SET idf = ? WHERE keyword_id = ?";

    /* url table operations */
    public static final String CREATE_URL_TABLE = "CREATE TABLE IF NOT EXISTS urls(url_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT UNIQUE, "
                                                   + "num_terms INTEGER, description TEXT, title TEXT)";
    public static final String TOTAL_NUM_URLS = "SELECT COUNT(url_id) FROM urls";
    public static final String GET_TOTAL_NUM_KEYWORDS_IN_URL = "SELECT num_words FROM urls WHERE url_id = ?";
    public static final String INSERT_URL = "INSERT OR IGNORE INTO urls(url, num_terms, description, title) VALUES (?, ?, ?, ?)";

    /* keywords table operations */                                                 
    public static final String CREATE_KEYWORDS_TABLE = "CREATE TABLE IF NOT EXISTS keywords(keyword_id INTEGER PRIMARY KEY AUTOINCREMENT, keyword TEXT UNIQUE)";
    public static final String GET_KEYWORD_ID = "SELECT keyword_id FROM keywords WHERE keyword = ?";
    public static final String INSERT_KEYWORDS = "INSERT OR IGNORE INTO keywords(keyword) VALUES (?)";
    public static final String GET_KEYWORD = "SELECT keyword FROM keywords WHERE keyword_id = ?";

}
