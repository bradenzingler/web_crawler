package com.java;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.Key;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class Site implements Comparable<Site> {
    private List<Keyword> keywords;
    private String url;
    private String title;
    private String description;
    private List<Double> embeddings;
    
    
    public Site(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public List<Keyword> getKeywords() {
        return this.keywords;
    }

    public String getDescription() {
        return this.description;
    }

    public String getTitle() {
        return this.title;
    }

    public List<Double> getEmbeddings() {
        return this.embeddings;
    }

    public Set<Site> extractLinks(Document doc) {
        Set<Site> sites = new HashSet<>();
        Elements els = doc.select("a[href]");

        for (org.jsoup.nodes.Element element : els) {
            String href = element.attr("abs:href");
            Site newSite = new Site(href);
            
            if (newSite.isValid()) {
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
        && !this.url.contains("Wikipedia:") && !this.url.contains("Portal:")
        && !this.url.contains("Talk:") && !this.url.contains("User:") && !this.url.contains("User_talk:")
        && !this.url.contains("#cite_note") && !this.url.contains("Template talk:");
    }


    public void extractData(Document doc, Lemmatizer lem) {
        String text = doc.select("p, h1, h2, h3, h4, h5, h6, title").text();
        String[] words = text.split(" ");
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
    private List<Keyword> filterKeywords(Lemmatizer lem, String[] words) {
        List<Keyword> filteredKeywords = new ArrayList<>();
        try {
            for (String word : words) {
                Keyword currKeyword = new Keyword(word);
                
                if (!currKeyword.isStopword() && currKeyword.isWord()) {
                    String newWord = lem.lemmatizeWord(currKeyword.toString());
                    Keyword newKeyword = new Keyword(newWord);
                    filteredKeywords.add(newKeyword);
                }
            }
        } catch (Exception e) {
            System.out.println("Error while filtering keywords: " + e.getStackTrace());
        }
        return filteredKeywords;
    }


    public Double getTermFrequency(Keyword keyword) {
        Double count = 0.0;
        for (Keyword k : this.keywords) {
            if (k.toString().equals(keyword.toString())) {
                count++;
            }
        }
        return count / this.keywords.size();
    }


    @Override
    public String toString() {
        return this.title;
    }

    @Override
    public int compareTo(Site o) {
       return this.url.equals(o.url) ? 1 : -1;
    }
}
