package com.java;

public class Main {
    public static void main(String[] args) {
        Crawler crawler = new Crawler("https://en.wikipedia.org/wiki/2024_Varzaqan_helicopter_crash");
        crawler.crawl();
    }
}
