package com.ir.youtubecommentindexer;

import co.elastic.clients.base.BooleanResponse;
import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.base.Transport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._core.*;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.indices.CreateRequest;
import co.elastic.clients.elasticsearch.indices.CreateResponse;
import co.elastic.clients.elasticsearch.indices.DeleteRequest;
import co.elastic.clients.elasticsearch.indices.DeleteResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ir.youtubecomment.YoutubeComment;

/**
 * An index pipeline that interacts with a cloud ElasticSearch
 * instance and does the following:
 * 1. Clean up existing data generated from previous runs.
 * 2. Create a new index.
 * 3. Read all Youtube comments from the CSV dataset.
 * $. Index the comments into the ElasticSearch instance. 
 */
public class YoutubeCommentIndexer {
    private static final Logger LOGGER = Logger.getLogger(YoutubeCommentIndexer.class.getName());

    // CSV data related constants.
    private static final String CSV_DATA_FILENAME = "data/youtube_dataset.csv";

    // Elastic search related constants.
    private static final String INDEX_NAME = "yt-comment-index";
    private static final String HOST_NAME = "ir-yt-comment.es.us-central1.gcp.cloud.es.io";
    private static final int PORT_NUMBER = 9243;
    private static final String USER_NAME = "elastic";
    private static final String PASSWORD = "M9CFbaZr6uOdWb9gk22FzgAy";

    private final ElasticsearchClient client;

    public YoutubeCommentIndexer() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, 
            new UsernamePasswordCredentials(USER_NAME, PASSWORD));
        RestClient restClient = RestClient.builder(
            new HttpHost(HOST_NAME, PORT_NUMBER, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)).build();
        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        // Add JavaTimeModule to enable parsing formatted date time string.
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());
        Transport transport = new RestClientTransport(restClient, jsonMapper);
        this.client = new ElasticsearchClient(transport);
    }

    /**
     * Delete the index generated from previous runs if it exists.
     * @return true when clean-up was successful.
     */
    public boolean cleanUp() {
        try {
            if (indexExists()) {
                DeleteRequest deleteRequest = new DeleteRequest.Builder().index(INDEX_NAME).build();
                DeleteResponse deleteResponse = client.indices().delete(deleteRequest);
                LOGGER.info("Delete index response: " + deleteResponse);
            }
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to clean up existing data. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Index all the Youtube comments stored in the CSV dataset.
     */
    public void indexComments() {
        List<YoutubeComment> comments = readCommentsFromCSVData();
        int indexedCommentCount = 0;
        for (YoutubeComment comment : comments) {
            IndexRequest<YoutubeComment> request = new IndexRequest.Builder<YoutubeComment>()
                .index(INDEX_NAME)
                .document(comment)
                .build();

            try {
                IndexResponse response = client.index(request);
                if (response.result() == Result.Created) {
                    indexedCommentCount++;
                    LOGGER.info(String.format("Finished indexing comment %s. Id: %s", comment.commentId, response.id()));
                    LOGGER.info(String.format("Indexed %d comments so far.", indexedCommentCount));
                } else {
                    LOGGER.warning(String.format("Failed to index comment %s. Response: %s", comment.commentId, response.toString()));
                }
            } catch (IOException e) {
                LOGGER.warning(String.format("Failed to index comment %s. Error: %s", comment.commentId, e.getMessage()));
            }
        }
        LOGGER.info(String.format("Finished indexing %d of %d comments.", indexedCommentCount, comments.size()));
    }

    /**
     * Check whether there is an existing index.
     * @return true when the index exists.
     */
    private boolean indexExists() {
        try {
            ExistsRequest existsRequest = new ExistsRequest.Builder().index(INDEX_NAME).build();
            BooleanResponse existsResponse = client.indices().exists(existsRequest);
            return existsResponse.value();
        } catch (IOException e) {
            LOGGER.severe("Failed to check whether the index exists. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a new index with a custom analyzer.
     * @return true when index creation was successful.
     */
    private boolean createIndex() {
        try {
            // Set up a standard analyzer with default stop words.
            Map<String, JsonData> settings = new HashMap<>();
            JsonObject analysis = Json.createObjectBuilder()
                .add("analyzer", Json.createObjectBuilder()
                    .add("standard_with_stop_words", Json.createObjectBuilder()
                        .add("tokenizer", "standard")
                        .add("filter", Json.createArrayBuilder()
                            .add("stop").build()
                        ).build()
                    ).build()
                ).build();
            settings.put("analysis", JsonData.of(analysis));
            CreateRequest createRequest = new CreateRequest.Builder().index(INDEX_NAME).settings(settings).build();
            CreateResponse createResponse = client.indices().create(createRequest);
            LOGGER.info("Create index response: " + createResponse);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to create new index. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Read all comments from the CSV dataset.
     * @return a list of comments.
     */
    private List<YoutubeComment> readCommentsFromCSVData() {
        List<YoutubeComment> comments = new ArrayList<>();
        
        int readCommentCount = 0;
        try {
            Reader in = new FileReader(CSV_DATA_FILENAME);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(YoutubeComment.CSV_HEADERS)
                .withFirstRecordAsHeader()
                .parse(in);
            for (CSVRecord record : records) {
                YoutubeComment comment = YoutubeComment.parseFromCSVRecord(record);
                comments.add(comment);
                readCommentCount++;
            }
        } catch (FileNotFoundException e) {
            LOGGER.severe("Could not find csv data file: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.severe("Failed to read csv data file: " + e.getMessage());
        }
        LOGGER.info(String.format("Finished reading %d comments.", readCommentCount));

        return comments;
    }
 
    public static void main(String[] args) {
        YoutubeCommentIndexer indexer = new YoutubeCommentIndexer();
        if (!indexer.cleanUp()) {
            System.exit(-1);
        }

        if (!indexer.createIndex()) {
            System.exit(-1);
        }
        
        indexer.indexComments();

        System.exit(0);
    }
}
