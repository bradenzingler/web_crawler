package com.java;

public class Statements {

    public static final String CREATE_URL_KEYWORDS_TABLE = "CREATE TABLE IF NOT EXISTS url_keywords("
    + "keyword TEXT UNIQUE, "
    + "idf REAL, "
    + "tfidf REAL, "
    + "term_frequency REAL, "
    + "url TEXT, "
    + "title TEXT, "
    + "description TEXT)";

    public static final String INSERT_VALUES = "INSERT OR IGNORE INTO url_keywords (keyword, term_frequency, url, title, description) VALUES (?, ?, ?, ?, ?)";

    public static final String TOTAL_NUM_DOCS = "SELECT COUNT(DISTINCT url) FROM url_keywords";
    public static final String GET_NUM_KEYWORDS = "SELECT COUNT(DISTINCT url) FROM url_keywords WHERE keyword = ?";
}
