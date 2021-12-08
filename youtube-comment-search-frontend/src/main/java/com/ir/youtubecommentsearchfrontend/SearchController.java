package com.ir.youtubecommentsearchfrontend;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import com.ir.youtubecommentsearchfrontend.model.SearchResult;
import com.ir.youtubecommentsearchfrontend.model.YoutubeCommentESClient;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The controller class for handling /search page.
 */
@Controller
public class SearchController {
    private static final Logger LOGGER = Logger.getLogger(SearchController.class.getName());

    private final YoutubeCommentESClient client;

    public SearchController(YoutubeCommentESClient client) {
        this.client = client;
    }

    @GetMapping(value="/search")
    public String runQuery(@RequestParam(value = "query") String query,
                           @RequestParam(value = "author") String author,
                           @RequestParam(value = "minDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate minDate,
                           @RequestParam(value = "minLikes") Integer minLikes,
                           @RequestParam(value = "startRank", defaultValue = "0") Integer startRank,
                           Model model) {
        LOGGER.info(String.format(
            "Received request for running query \"%s\" with author %s, date %s, minLikes %d, startRank %d", 
            query, author, minDate, minLikes, startRank));
        model.addAttribute("query", query);
        model.addAttribute("author", author);
        model.addAttribute("minDate", minDate);
        model.addAttribute("minLikes", minLikes);
        model.addAttribute("startRank", startRank);

        List<SearchResult> results = client.searchComment(query, author, minDate, minLikes, startRank);

        model.addAttribute("results", results);
        model.addAttribute("resultSize", results.size());
        
        return "search";
    }
}
