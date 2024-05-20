package com.java;

public class Statements {

    public static final String CREATE_URL_KEYWORDS_TABLE = "CREATE TABLE IF NOT EXISTS url_keywords("
    + "PRIMARY KEY embeddings TEXT, "
    + "url TEXT UNIQUE, "
    + "title TEXT, "
    + "description TEXT)";

    public static final String INSERT_VALUES = "INSERT OR IGNORE INTO url_keywords (embeddings, url, title, description) VALUES (?, ?, ?)";

}
