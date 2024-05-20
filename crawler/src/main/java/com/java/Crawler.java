/**
 * A web crawler that extracts keywords, descriptions, and titles from Wikipedia pages.
 * The keywords are then lemmatized and output to a relational database, along with the urls.
 * Braden Zingler
 */

//package com.java;

// import org.jsoup.Jsoup;
// import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import javax.swing.text.Document;


public class Crawler {
    private Queue<Site> urlsToVisit;
    private ArrayList<Site> visitedUrls;
    public Lemmatizer lem;
    private Database db;


    /**
     * Creates a new Crawler object and instantiates its properties.
     * @param startUrl the first url to start crawling with.
     */
    public Crawler(String startUrl) {
        Site firstSite = new Site(startUrl);
        this.urlsToVisit = new PriorityQueue<>();
        this.db = new Database();
        this.urlsToVisit.add(firstSite);
        this.visitedUrls = new ArrayList<>();
        lem = new Lemmatizer();
    }


    /**
     * Starts crawling websites in the queue. Adds the extracted data to the database.
     */
    public void crawl() {
        while (!this.urlsToVisit.isEmpty()) {
            Site currSite = this.urlsToVisit.poll();
            if (currSite.isValid() && !this.visitedUrls.contains(currSite) && currSite.isAllowedByRobotsTxt()) {
                try {
                    Document doc = Jsoup.connect(currSite).get();
                    doc.outputSettings().charset("UTF-8");

                    currSite.extractData(doc, this.lem);
                    db.sendToDatabase(currSite);
                    this.visitedUrls.add(currSite);
                } catch (Exception e) {
                    System.out.println("Failed on site: " + currSite + ": " + e);
                }
            }
        }
    }
}
