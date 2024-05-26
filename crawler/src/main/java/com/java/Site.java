package com.java;
import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;


public class Site implements Comparable<Site> {
    private Map<Keyword, Double> keywords;
    private String url;
    private String title;
    private String description;
    
    
    public Site(String url) {
        this.url = url;
        this.keywords = new HashMap<>();
    }

    public String getUrl() {
        return this.url;
    }

    public Map<Keyword, Double> getKeywords() {
        return this.keywords;
    }

    public String getDescription() {
        return this.description;
    }

    public String getTitle() {
        return this.title;
    }


    public Set<Site> extractLinks(Document doc) {
        Set<Site> sites = new HashSet<>();
        Elements els = doc.select("a[href]");

        for (Element element : els) {
            String href = element.attr("abs:href");
            Site newSite = new Site(href);
            
            if (newSite.isValid() && sites.size() < 10) {
                sites.add(newSite);
            }
        }
        return sites;
    }


    public boolean isValid() {
        return this.url.startsWith("https://en.wikipedia.org/wiki")
        && !this.url.contains("Main_Page") && !this.url.contains("Special:")
        && !this.url.contains("Help:") && !this.url.contains("File:")
        && !this.url.contains("Template:") && !this.url.contains("Category:")
        && !this.url.contains("Portal:") && !this.url.contains("Wikipedia:")
        && !this.url.contains("Talk:") && !this.url.contains("User:") && !this.url.contains("User_talk:")
        && !this.url.contains("#cite_note") && !this.url.contains("Template talk:") 
        && !this.url.contains("#") && !this.url.startsWith("https://en.wikipedia.org/wiki/1")
        && !this.url.startsWith("https://en.wikipedia.org/wiki/2");
    }


    public void extractData(Document doc, Lemmatizer lem) {
        String text = doc.select("p, h1, h2, h3, h4, h5, h6, title").text();
        String[] words = text.split("\\s+");
        extractLinks(doc);

        this.keywords = filterKeywords(lem, words);
        this.title = doc.title();
        this.description = doc.select("#mw-content-text > div.mw-content-ltr.mw-parser-output > div.shortdescription.nomobile.noexcerpt.noprint.searchaux").text();
    }


    /**
     * Filters out the stopwords from the list of keywords for a site.
    * @param words The list of words
    * @return The list of words without the stopwords
    */
    private Map<Keyword, Double> filterKeywords(Lemmatizer lem, String[] words) {
        Map<Keyword, Double> filteredKeywords = new HashMap<>();

        try {
            for (String word : words) {
                Keyword currKeyword = new Keyword(word.toLowerCase().strip());
                
                if (!currKeyword.isStopword() && currKeyword.isWord() && !word.matches("\\d+") && word.length() > 1){
                    String newWord = lem.lemmatizeWord(currKeyword.toString());
                    Keyword newKeyword = new Keyword(newWord);
                    filteredKeywords.put(newKeyword, filteredKeywords.getOrDefault(newKeyword, 0.0) + 1.0);
                }
            }

        } catch (Exception e) {
            System.out.println("Error while filtering keywords: " + e.getStackTrace());
        }
        return filteredKeywords;
    }


     /**
     * Normalizes a URL to ensure consistent representation.
     * @param url the url to be normalized
     * @return the normalized url
     */
    public String getNormalizedUrl() {
        try {
            URI uri = new URI(this.url);
            String normalizedUrl = uri.normalize().toString();
            return normalizedUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return Objects.equals(url, site.url);
    }


    @Override
    public String toString() {
        return this.title;
    }

    @Override
    public int compareTo(Site o) {
        return this.getNormalizedUrl().compareTo(o.getNormalizedUrl());
    }
}
