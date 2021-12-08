package com.ir.youtubecommentsearchfrontend.model;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ir.youtubecomment.YoutubeComment;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.base.Transport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._core.SearchRequest;
import co.elastic.clients.elasticsearch._core.SearchResponse;
import co.elastic.clients.elasticsearch._core.search.Highlight;
import co.elastic.clients.elasticsearch._core.search.HighlightField;
import co.elastic.clients.elasticsearch._core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * A client class that searches Youtube comments by making requests to
 * a cloud ElasticSearch instance.
 */
@Component
public class YoutubeCommentESClient {
    private static final Logger LOGGER = Logger.getLogger(YoutubeCommentESClient.class.getName());

    private static final int SEARCH_RESULT_PAGE_SIZE = 10;

    // Elastic search related constants.
    private static final String INDEX_NAME = "yt-comment-index";
    private static final String HOST_NAME = "ir-yt-comment.es.us-central1.gcp.cloud.es.io";
    private static final int PORT_NUMBER = 9243;
    private static final String USER_NAME = "elastic";
    private static final String PASSWORD = "M9CFbaZr6uOdWb9gk22FzgAy";

    private final ElasticsearchClient client;

    public YoutubeCommentESClient() {
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
     * Search the most relevant comments from ElasticSearch.
     * @param queryText the query text containing terms from comment and video name.
     * @param author the text containing terms from the comment author name.
     * @param minDate the earliest date after which the comment was posted.
     * @param minLikes the minimum number of likes the comment had.
     * @param startRank the starting rank for the returned search results.
     * @return a list of results based on the search inputs.
     */
    public List<SearchResult> searchComment(
        String queryText, String author, LocalDate minDate, int minLikes, int startRank) {
        List<SearchResult> results = new ArrayList<>();
        
        // The final compound query consists of multiple child queries, each of which covers
        // a subset of the fields.
        BoolQuery.Builder compoundQueryBuilder = new BoolQuery.Builder()
            .addMust(new Query.Builder().multiMatch(getTextMultiMatchQuery(queryText)).build())
            .addShould(new Query.Builder().match(getUserNameMatchQuery(author)).build())
            .addFilter(new Query.Builder().range(getDateRangeQuery(minDate)).build())
            .addFilter(new Query.Builder().range(getLikesRangeQuery(minLikes)).build());
        if (author != null && !author.isEmpty()) {
            compoundQueryBuilder.addShould(new Query.Builder().match(getUserNameMatchQuery(author)).build());
        }

        // Finalze the search request.
        SearchRequest request = new SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(new Query.Builder().bool(compoundQueryBuilder.build()).build())
            .sort(getSortOption())
            .highlight(getHighlightOption())
            .from(startRank)
            .size(SEARCH_RESULT_PAGE_SIZE)
            .build();
        try {
            SearchResponse<YoutubeComment> response = client.search(request, YoutubeComment.class);
            List<Hit<YoutubeComment>> hits = response.hits().hits();
            for (int i = 0; i < hits.size(); i++) {
                Hit<YoutubeComment> hit = hits.get(i);
                SearchResult result = new SearchResult(hit.source(), hit.score(), hit.highlight());
                results.add(result);
            }
            LOGGER.info("Receiving search response: " + response);
        } catch (IOException e) {
            LOGGER.severe("Failed to search ElasticSearch. Error: " +e.getMessage());
        }

        return results;
    }

    /**
     * Construct a multi-match query that searches comment and video name fields.
     * @param queryText the given query text.
     * @return the multi-match query.
     */
    private MultiMatchQuery getTextMultiMatchQuery(String queryText) {
        return new MultiMatchQuery.Builder()
            .query(queryText)
            .fields(YoutubeComment.QUERIABLE_HEADERS)
            .fuzziness("AUTO")  // Enable fuzziness.
            .analyzer("standard_with_stop_words")  // Enable filtering out stop words.
            .build();
    }

    /**
     * Construct a match query that searches comment author field.
     * @param userName the author name text.
     * @return the match query.
     */
    private MatchQuery getUserNameMatchQuery(String userName) {
        return new MatchQuery.Builder()
            .query(userName)
            .field(YoutubeComment.USER_NAME_FIELD)
            .fuzziness("AUTO")  // Enable fuzziness.
            .analyzer("standard_with_stop_words")  // Enable filtering out stop words.
            .build();
    }

    /**
     * Construct a filter query to filter on query date.
     * @param minDate the earliest date to fitler.
     * @return the filter query in JSON format.
     */
    private JsonObject getDateRangeQuery(LocalDate minDate) {
        return Json.createObjectBuilder()
            .add(YoutubeComment.DATE_FIELD, Json.createObjectBuilder()
            .add("gte", minDate.toString()))
            .build();
    }

    /**
     * Construct a filter query to filter on number of likes.
     * @param minLikes the minimum number of likes to filter.
     * @return the filter query in JSON format.
     */
    private JsonObject getLikesRangeQuery(int minLikes) {
        return Json.createObjectBuilder()
            .add(YoutubeComment.LIKES_FIELD, Json.createObjectBuilder()
            .add("gte", minLikes))
            .build();
    }

    /**
     * Construct a sort option to sort by both relevance score
     * and number of likes.
     * @return the sort option in JSON format.
     */
    private JsonArray getSortOption() {
        return Json.createArrayBuilder()
            .add("_score")
            .add(Json.createObjectBuilder()
                .add(YoutubeComment.LIKES_FIELD, "desc"))
            .build();
    }

    /**
     * Construct a highlight option to highlight matching text
     * in comment and video name fields.
     * @return the highlight option.
     */
    private Highlight getHighlightOption() {
        Map<String, HighlightField> highlightFields = new HashMap<>();
        // Set number of fragments to 0 to avoid breaking text fields into segments.
        for (String fieldName : YoutubeComment.QUERIABLE_HEADERS) {
            highlightFields.put(fieldName, new HighlightField.Builder().numberOfFragments(0).build());
        }
        highlightFields.put(
            YoutubeComment.USER_NAME_FIELD, new HighlightField.Builder().numberOfFragments(0).build());
        return new Highlight.Builder().fields(highlightFields).build();
    }
    
}
