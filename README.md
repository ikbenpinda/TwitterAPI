# TwitterAPI
TwitterAPI module for Java.

Version 0.1 // hacked-together-and-ported proof-of-concept edition.

Setup

To import this API make sure you've included:
- Scribe
- jsonSimple
- javax ssl

Usage

TwitterFeed is made to be simple to use. It's easy to work with - but with less functionality.
To use the TwitterFeed simply do the following:

```java
TwitterFeed feed = new TwitterFeed();
```

From here on you can call all it's functions, e.g.

```java
String status = feed.getStatus();
```
This will return the amount of requests you have left.
