# web_crawler

A web crawler built in Java to gather website keywords for an inverted index search engine. Compliant with the Robots Exclusion Protocol.

The web crawler gathers keywords, lemmatizes them, makes an OpenAI API call to get their word vectors, and adds them to an sqlite database with an index.
It also converts the word vectors to their phasor form in the complex plane to avoid dumping massive vectors in the database, and simplify computations for the search engine.

Currently, it is restricted to Wikipedia pages to give better results for a search engine, but it can be changed slightly to work on any website on the internet.
