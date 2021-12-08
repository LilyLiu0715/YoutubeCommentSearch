package com.ir.youtubecomment;

import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.csv.CSVRecord;

/**
 * A data model class that represents a youtube comment.
 */
public class YoutubeComment {
    private static final Logger LOGGER = Logger.getLogger(YoutubeComment.class.getName());

    // CSV related constants.
    public static final String CSV_VIDEO_NAME_HEADER = "Video Name";
    public static final String CSV_CHANNEL_NAME_HEADER = "Channel Name";
    public static final String CSV_COMMENT_ID_HEADER = "Comment Id";
    public static final String CSV_USER_NAME_HEADER = "User Name";
    public static final String CSV_COMMENT_HEADER = "Comment";
    public static final String CSV_DATE_HEADER = "Date";
    public static final String CSV_LIKES_HEADER = "Likes";
    
    public static final String[] CSV_HEADERS = {CSV_VIDEO_NAME_HEADER, CSV_CHANNEL_NAME_HEADER, CSV_COMMENT_ID_HEADER, 
        CSV_USER_NAME_HEADER, CSV_COMMENT_HEADER, CSV_DATE_HEADER, CSV_LIKES_HEADER};

    // JSON related constants.
    public static final String VIDEO_NAME_FIELD = "VideoName";
    public static final String CHANNEL_NAME_FIELD = "ChannelName";
    public static final String COMMENT_ID_FIELD = "CommentId";
    public static final String USER_NAME_FIELD = "UserName";
    public static final String COMMENT_FIELD = "Comment";
    public static final String DATE_FIELD = "Date";
    public static final String LIKES_FIELD = "Likes";

    public static final String[] QUERIABLE_HEADERS = {VIDEO_NAME_FIELD, CHANNEL_NAME_FIELD, COMMENT_FIELD};

    @JsonProperty(VIDEO_NAME_FIELD)
    public String videoName;
    
    @JsonProperty(CHANNEL_NAME_FIELD)
    public String channelName;

    @JsonProperty(COMMENT_ID_FIELD)
    public String commentId;
    
    @JsonProperty(USER_NAME_FIELD)
    public String userName;
    
    @JsonProperty(COMMENT_FIELD)
    public String comment;

    @JsonProperty(DATE_FIELD)
    public String date;
    
    @JsonProperty(LIKES_FIELD)
    public int likes;

    public YoutubeComment() {}

    @JsonCreator
    public YoutubeComment(@JsonProperty(VIDEO_NAME_FIELD) String videoName,
                          @JsonProperty(CHANNEL_NAME_FIELD) String channelName,
                          @JsonProperty(COMMENT_ID_FIELD) String commentId,
                          @JsonProperty(USER_NAME_FIELD) String userName,
                          @JsonProperty(COMMENT_FIELD) String comment,
                          @JsonProperty(DATE_FIELD) String date,
                          @JsonProperty(LIKES_FIELD) int likes) {
        this.videoName = videoName;
        this.channelName = channelName;
        this.commentId = commentId;
        this.userName = userName;
        this.comment = comment;
        this.date = date;
        this.likes = likes;
    }

    /**
     * Generates a Youtube comment from a CSV record.
     * @param csvRecord the input CSV record.
     * @return the generated Youtube comment.
     */
    public static YoutubeComment parseFromCSVRecord(CSVRecord csvRecord) {
        YoutubeComment comment = new YoutubeComment();

        String commentId = csvRecord.get(CSV_COMMENT_ID_HEADER);
        comment.commentId = commentId;

        String videoName = csvRecord.get(CSV_VIDEO_NAME_HEADER);
        comment.videoName = videoName;

        String channelName = csvRecord.get(CSV_CHANNEL_NAME_HEADER);
        comment.channelName = channelName;
        
        String userName = csvRecord.get(CSV_USER_NAME_HEADER);
        comment.userName = userName;
        
        String commentText = csvRecord.get(CSV_COMMENT_HEADER);
        comment.comment = commentText;

        String date = csvRecord.get(CSV_DATE_HEADER);
        comment.date = date;

        try {
            int likes = Integer.parseInt(csvRecord.get(CSV_LIKES_HEADER));
            comment.likes = likes;
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format("Failed to parse likes from %s. Error: %s", 
                csvRecord.get(CSV_LIKES_HEADER), e.getMessage()));
        }

        return comment;
    }
}

