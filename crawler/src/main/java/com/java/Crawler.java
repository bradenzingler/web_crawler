/**
 * A web crawler that extracts keywords, descriptions, and titles from Wikipedia pages.
 * The keywords are then lemmatized and output to a relational database, along with the urls.
 * Braden Zingler
 */

package com.java;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;


public class Crawler {
    private Queue<Site> urlsToVisit;
    private Set<String> visitedUrls;
    public Lemmatizer lem;
    private Database db;


    /**
     * Creates a new Crawler object and instantiates its properties.
     * @param startUrl the first url to start crawling with.
     */
    public Crawler(String startUrl) {
        Site firstSite = new Site(startUrl);
        this.urlsToVisit = new PriorityQueue<>();
        this.urlsToVisit.add(firstSite);
        this.urlsToVisit.add(new Site("https://en.wikipedia.org/wiki/Deep_time"));
        this.urlsToVisit.add(new Site("https://en.wikipedia.org/wiki/Evolution"));
        this.urlsToVisit.add(new Site("https://en.wikipedia.org/wiki/Scientific_theory"));
        this.urlsToVisit.add(new Site("https://en.wikipedia.org/wiki/Engineering"));
        this.urlsToVisit.add(new Site("https://en.wikipedia.org/wiki/Machine"));

        this.db = new Database();
        this.visitedUrls = new HashSet<>();
        lem = new Lemmatizer();
    }


    /**
     * Starts crawling websites in the queue. Adds the extracted data to the database.
     */
    public void crawl() {
        int count = 0;
        while (!this.urlsToVisit.isEmpty()) {
            Site currSite = this.urlsToVisit.poll();
            if (currSite.isValid() && !this.visitedUrls.contains(currSite.getUrl())) {
                try {
                    Document doc = Jsoup.connect(currSite.getUrl()).timeout(2000).get();
                    doc.outputSettings().charset("UTF-8");
                    
                    // Add discovered sites to the queue
                    Set<Site> discoveredSites = currSite.extractLinks(doc);
                    for (Site site : discoveredSites) {
                        if (!this.visitedUrls.contains(site.getUrl())) {
                            this.urlsToVisit.add(site);
                        }
                    }
                    currSite.extractData(doc, this.lem);
                    db.sendToDatabase(currSite);
                    this.visitedUrls.add(currSite.getUrl());
                    count++;
                    System.out.println(count + " sites visited: " + currSite.getTitle());
                } catch (Exception e) {
                    System.out.println("Failed on site: " + currSite + ": " + e);
                }
            } else {
                this.urlsToVisit.remove(currSite);
            }
        }
        db.closeConnection();
    }
}
