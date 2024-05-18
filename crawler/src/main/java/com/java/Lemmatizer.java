package com.java;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;


public class Lemmatizer {
    public HashMap<String, String> lemmas;

    public Lemmatizer() {
        this.lemmas = new HashMap<>();
        readLemmaList();
    }


    /**
     * Lemmatizes a keyword.
     * @param word the word to be lemmatized
     * @return the lemmatized version of the word
     */
    public String lemmatizeWord(String word) {
        if (this.lemmas.containsKey(word)) {
            return this.lemmas.get(word);
        }
        return word;
    }

    
    /**
     * Reads the lemmatizations_list.txt file into a HashMap before starting the web scraping.
     */
    public void readLemmaList() {
        try {
            File f = new File("crawler/src/main/resources/lemmatization_list.csv");
            Scanner scnr = new Scanner(f);
            while (scnr.hasNextLine()) { 
                String line = scnr.nextLine();
                String[] parts = line.split(",");
                this.lemmas.put(parts[1].strip(), parts[0].strip());
            }
            System.out.println("Lemmas read into hashmap successfully.");
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
    
}
