package com.java;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.panforge.robotstxt.RobotsTxt;        //https://github.com/pandzel/RobotsTxt
 
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import javax.print.Doc;
import java.lang.Thread;


public class Crawler {

    public static void main(String[] args) {
        try {

            // Initialize web crawler, input/output files
            File unvisitedCSV = new File("crawler/src/main/resources/small.csv");
            File output = new File("crawler/src/main/resources/output.csv");
            Scanner scnr = new Scanner(unvisitedCSV);
            FileWriter writer = new FileWriter(unvisitedCSV, true);
            FileWriter out = new FileWriter(output, true);
            ArrayList<String> visited = new ArrayList<>();
            
            // Read through all site urls
            while (scnr.hasNextLine()) {

                // Get site url from csv
                String[] parts = scnr.nextLine().split(",");
                String stringUrl = parts[1].replace("\"", "");

                try {
                    if (!visited.contains(stringUrl)) {
                        visited.add(stringUrl);
                        String url = stringUrl.contains("https") ? stringUrl :  "https://" + stringUrl;
                        
                        // Check robots.txt
                        if (!isAllowedByRobotsTxt(url)) continue;
                        Document doc = Jsoup.connect(url).get();

                        // Get keywords
                        String text = extractText(doc);
                        String[] words = text.split("\\s+");
                        List<String> keywords = filterKeywords(words);
                        Map<String, Integer> frequencyMap = countFrequency(keywords);
                        List<String> relevantKeywords = selectKeywords(frequencyMap);

                        // Output information parsed to output.csv
                        out.append(stringUrl+ ", " + doc.title() + ", " + relevantKeywords.toString() + "\n");
                        out.flush();
                        
                        // Add any discovered links to the csv to continue scraping
                        // This will add discovered links to the end of the queue.
                        // Set<String> links = extractLinks(doc);
                        // for (String link : links) {
                        //     writer.append("fill," + link + ",fill\n");
                        //     writer.flush();
                        // }

                        System.out.println("Scraped " + url);
                    }
                } catch(Exception e) {
                    System.out.println("Error: " + stringUrl);
                }
            }
                out.close();
                writer.close();
        }  catch(Exception e) {
            System.out.println("Failed to read/write file: "+e);
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
     * Extract the text from the document
     * @param doc The document to extract the text from
     * @return The text extracted from the document
     */
    private static String extractText(Document doc) {
        StringBuilder textBuilder = new StringBuilder();
        Elements elements = doc.select("p, h1, h2, h3, h4, h5, h6, title");
        for (Element element : elements) {
            textBuilder.append(element.text()).append(" ");
        }
        return textBuilder.toString();
    }


    /**
     * Count the frequency of each keyword
     * @param keywords The list of keywords
     * @return The frequency map of the keywords
     */
    private static Map<String, Integer> countFrequency(List<String> keywords) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String keyword : keywords) {
            frequencyMap.put(keyword, frequencyMap.getOrDefault(keyword, 0) + 1);
        }
        return frequencyMap;
    }


    /**
     * Extract all the links from the document to crawl
     * @param doc The document to extract the links from
     * @return The set of links
     */
    private static Set<String> extractLinks(Document doc) {
        Set<String> links = new HashSet<>();
        Elements elements = doc.select("a[href]");
        for (Element element : elements) {
            String absUrl = element.absUrl("href");
            links.add(absUrl);
        }
        return links;
    }


    /**
     * Filter out the stopwords from the list of words
     * @param words The list of words
     * @return The list of words without the stopwords
     */
    private static List<String> filterKeywords(String[] words) {
        List<String> filteredKeywords = new ArrayList<>();
        for (String word : words) {
            if (!isStopword(word)) {
                filteredKeywords.add(word);
            }
        }
        return filteredKeywords;
    }

    
    /**
     * Select the top 10 keywords with the highest frequency
     * @param frequencyMap The frequency map of the keywords
     * @return  The top 10 keywords with the highest frequency
     */
    private static List<String> selectKeywords(Map<String, Integer> frequencyMap) {
        List<String> relevantKeywords = new ArrayList<>(frequencyMap.keySet());
        relevantKeywords.sort(Comparator.comparingInt(frequencyMap::get).reversed());
        return relevantKeywords.subList(0, Math.min(relevantKeywords.size(), 7));
    }


    /**
     * Check if a word is a stopword
     * @param word The word to check
     * @return True if the word is a stopword, false otherwise
     */
    private static boolean isStopword(String word) {
        String[] stopWords = new String[] {"a","about","above","according","across","actually","after","again","against","all","almost","also","although","always","am","among","amongst","an","and","any","anything","anyway","are","as","at","be","became","become","because","been","before","being","below","between","both","but","by","can","could","did","do","does","doing","down","during","each","either","else","few","for","from","further","had","has","have","having","he","he'd","he'll","hence","he's","her","here","here's","hers","herself","him","himself","his","how","how's","I","I'd","I'll","I'm","I've","if","in","into","is","it","it's","its","itself","just","let's","may","maybe","me","might","mine","more","most","must","my","myself","neither","nor","not","of","oh","on","once","only","ok","or","other","ought","our","ours","ourselves","out","over","own","same","she","she'd","she'll","she's","should","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there","there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","we","we'd","we'll","we're","we've","were","what","what's","when","whenever","when's","where","whereas","wherever","where's","whether","which","while","who","whoever","who's","whose","whom","why","why's","will","with","within","would","yes","yet","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"};
        for (String stopWord : stopWords) {
            if (word.equalsIgnoreCase(stopWord)) return true;
        }
        return false;
    }
}