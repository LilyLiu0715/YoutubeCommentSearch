package com.ir.youtubecommentsearchfrontend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import com.ir.youtubecomment.YoutubeComment;

/**
 * A data model class that represents a search result.
 */
public class SearchResult {
    private static final int NUM_SCORE_DECIMALS = 3;

    private YoutubeComment comment;

    // Relevance score determined by ElasticSearch.
    private double score;

    // Highlights are keyed by field name.
    private Map<String, List<String>> highlights;

    public SearchResult(YoutubeComment comment, double score, Map<String, List<String>> highlights) {
        this.comment = comment;
        this.score = score;
        this.highlights = highlights;
    }

    public String displayScore() {
        BigDecimal bd = new BigDecimal(Double.toString(score));
        bd = bd.setScale(NUM_SCORE_DECIMALS, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public String displayHighlightedUserName() {
        if (highlights.containsKey(YoutubeComment.USER_NAME_FIELD)) {
            return highlights.get(YoutubeComment.USER_NAME_FIELD).get(0);
        }

        return comment.userName;
    }

    public String displayHighlightedComment() {
        if (highlights.containsKey(YoutubeComment.COMMENT_FIELD)) {
            return highlights.get(YoutubeComment.COMMENT_FIELD).get(0);
        }

        return comment.comment;
    }

    public String displayHighlightedVideoName() {
        if (highlights.containsKey(YoutubeComment.VIDEO_NAME_FIELD)) {
            return highlights.get(YoutubeComment.VIDEO_NAME_FIELD).get(0);
        }

        return comment.videoName;
    }

    public String displayChannelName() {
        return comment.channelName;
    }

    public String displayDate() {
        return comment.date;
    }

    public String displayLikes() {
        return Integer.toString(comment.likes);
    }
}
