package com.java;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.Key;
import java.util.*;
import javax.swing.text.Document;
// import com.panforge.robotstxt.RobotsTxt;        //https://github.com/pandzel/RobotsTxt


public class Site {
    private List<Keyword> keywords;
    private String url;
    private String title;
    private String description;
    private List<Double> embeddings;
    
    
    public Site(String url) {
        this.url = url.contains("https") ? url :  "https://" + url;
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


    public boolean isValid() {
        return this.url.startsWith("https://en.wikipedia.org/wiki")
        && !this.url.contains("Main_Page") && !this.url.contains("Special:")
        && !this.url.contains("Help:") && !this.url.contains("File:")
        && !this.url.contains("Template:") && !this.url.contains("Category:")
        && !this.url.contains("Wikipedia:") && !this.url.contains("Portal:")
        && !this.url.contains("Talk:") && !this.url.contains("User:") && !this.url.contains("User_talk:");
    }

    
    public void extractData(Document doc, Lemmatizer lem) {
        //String text = doc.select("p, h1, h2, h3, h4, h5, h6, title").text();
        //String[] words = text.split(" ");

        //this.keywords = filterKeywords(lem, words);
        //this.title = doc.title();
        //this.description = doc.select("meta[name=description]").attr("content");
    }


    /**
     * Filters out the stopwords from the list of keywords for a site.
    * @param words The list of words
    * @return The list of words without the stopwords
    */
    private List<String> filterKeywords(Lemmatizer lem, String[] words) {
        List<String> filteredKeywords = new ArrayList<>();
        try {
            
            for (String word : words) {
                Keyword currKeyword = new Keyword(word);
                
                if (!currKeyword.isStopword() && currKeyword.isWord()) {
                    String newWord = lem.lemmatizeWord(currKeyword.toString());
                    filteredKeywords.add(newWord);
                }
            }
        } catch (Exception e) {
            System.out.println("Error while filtering keywords: " + e.getStackTrace());
        }
        return filteredKeywords;
    }



    public List<List<Double>> getEmbeddings() {
        try {
            OpenAiService service = new OpenAiService(System.getenv("OPENAI_API_KEY"));
            EmbeddingRequest request = new EmbeddingRequest.builder().input(this.keywords).model("text-embedding-3-small").build();
            EmbeddingResult result = service.createEmbeddings(request);
            this.embeddings = result.getData();
            


        }

    }


    /**
    * Checks that a website allows web scraping.
    * @param targetUrl the url to check.
    * @return true if the website allows web scraping, false otherwise.
    */
    public boolean isAllowedByRobotsTxt() {
        try {
            URI url = new URI(this.url);
            String robots = "https://" + url.getHost() + "/robots.txt";
            URI robotsUrl = new URI(robots);

            HttpURLConnection connection = (HttpURLConnection) robotsUrl.openConnection();
            connection.setReadTimeout(2000);
            RobotsTxt robotsTxt = RobotsTxt.read(connection.getInputStream());
            return robotsTxt.query("scraper", robotsUrl);
        } catch (Exception e) {
            System.out.println(targetUrl + " disallowed scraping: " + e.getMessage());
        }
        return false;
    }
}
