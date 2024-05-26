package com.java;

import java.util.Objects;

public class Keyword {
    private String word;

    public Keyword(String word) { 
        this.word = word.toLowerCase().strip();
    }

    public String getWord() {
        return this.word;
    }

    

    /**
    * Checks if the current word is a word and does not contain special characters.
    * @param word the word to check.
    * @return true if doesn't contain the ignored characters, false otherwise.
    */
    public boolean isWord() {
        try {
            return this.word.matches("\\b[a-zA-Z]+\\b");
        } catch (Exception e) {
            System.out.println("An error occurred while checking if " + word + " is a word: " + e);
        }
        return false;
    }


    /**
    * Check if a keyword is a stopword
    * @param word The word to check
    * @return True if the word is a stopword, false otherwise
    */
    public boolean isStopword() {
        try {
            String[] stopWords = new String[] {"a", "afterwords", "about","above","according","across","actually","after","again","against","all","almost","also","although","always","am","among","amongst","an","and","any","anything","anyway","are","as","at","be","became","become","because","been","before","being","below","between","both","but","by","can","could","did","do","does","doing","down","during","each","either","else","few","for","from","further","had","has","have","having","he","he'd","he'll","hence","he's","her","here","here's","hers","herself","him","himself","his","how","how's","I","I'd","I'll","I'm","I've","if","in","into","is","it","it's","its","itself","just","let's","may","maybe","me","might","mine","more","most","must","my","myself","neither","nor","not","of","oh","on","once","only","ok","or","other","ought","our","ours","ourselves","out","over","own","same","she","she'd","she'll","she's","should","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there","there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","we","we'd","we'll","we're","we've","were","what","what's","when","whenever","when's","where","whereas","wherever","where's","whether","which","while","who","whoever","who's","whose","whom","why","why's","will","with","within","would","yes","yet","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves", "wikipedia", "use"};
            for (String stopWord : stopWords) {
                if (this.word.equalsIgnoreCase(stopWord)) return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("An error occurred while checking if is stopword: " + e);
        }
        return true;
    }


    /**
     * Compares two keywords based on their word.
     * @param other the other keyword to compare to.
     * @return true if the words are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keyword keyword = (Keyword) o;
        return this.word.equals(keyword.word);
    }


    /**
     * Computes a hash code for the keyword.
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.word);
    }


    /**
     * Converts a keyword to a string.
     * @return the keyword as a string.
     */
    @Override
    public String toString() {
        return this.word;
    }
}
