# Youtube Comment Search - CS 6200 Final Project User Guide

## Project structure

This project has two components (see design in final project report):

* Youtube comment indexer, implemented as a Java Maven project.
* Youtube comment search frontend, implemented as a Spring Boot project.

Github repository link: <https://github.com/LilyLiu0715/YoutubeCommentSearch>

### Youtube Comment Indexer

```bash
youtube-document-indexer
├── data/
│    ├── youtube_dataset.csv
├── pom.xml
├── src
│    ├── main/java/com/ir
│        ├── youtubecomment/
│        |       ├── YoutubeComment.java
│        ├── youtubecommentindexer/
│        |       ├── YoutubeCommentIndexer.java
│        ├── resources/
│            ├── log4j.properties
├── target
     ├── youtube-comment-indexer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Youtube Comment Search Frontend

```bash
youtube-comment-search-frontend
├── pom.xml
├── src
│    ├── main/
│        ├── appengine/
│        │   ├── app.yaml
│        ├── java/
│        │   ├── com/ir/
│        |       ├── youtubecomment/
│        |       |      ├── YoutubeComment.java
│        |       ├── youtubecommentsearchfrontend/
│        |              ├── SearchController.java
│        |              ├── YoutubeCommentSEarchFrontendApplication.java
│        |              ├── model/
│        |                    ├── SearchResult.java
│        |                    ├── YoutbueCommentESClient.java
│        ├── resources/
│            ├── application.properties 
│            ├── static/
│                 ├── index.html
│            ├── templates/
│                 ├── search.html
├── target
     ├── youtube-comment-search-frontend-0.0.1-SNAPSHOT.jar
```

## Deployment & Testing

The indexer only needs to run once locally to index all the comments into the cloud ElasticSearch instance, which has been done.
Subsequent runs will overwrite the previous data and hence are no-op.

The search frontend is already deployed on Google Cloud Platform. To access the web application, go the the following url:

<https://youtubecommentsearch-334306.uc.r.appspot.com/>

Note that the cloud ElasticSearch instance is during the free trial period, which will expire on 12/16/2021. Please let me know if access after this date is needed.
