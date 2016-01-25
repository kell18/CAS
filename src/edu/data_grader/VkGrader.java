package edu.data_grader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.Optional;

import edu.json.JSONArray;
import edu.json.JSONObject;

public class VkGrader {
    private static final String API_URL = "https://api.vk.com/method/";
    private static final String GATHER_COMMENTS_METHOD = "wall.getComments";
    private static final String GATHER_POSTS_METHOD = "wall.get";
    private static final String ERROR_RETVAL = "error";
    private static final int MAX_CHUNK_SIZE = 100;
    private static final String API_VERSION = "5.44";

    private int VisitedPosts = 0;
    private long postsGathered = 0;

    public Optional<JSONArray> GatherComments(final long ownerId, int postsCount) throws IOException, NoSuchElementException {
        String commentsStr;
        JSONArray comments = new JSONArray();
        JSONObject responseObj;
        JSONArray posts = GatherPosts(ownerId, postsCount).orElseThrow(
                () -> new NoSuchElementException("Posts gather failed.")
        );
        postsCount = posts.length();
        for (int i = 0; i < postsCount; i++) {
            final int postNum = i;
            commentsStr = GatherData(GATHER_COMMENTS_METHOD, new HashMap<String, String>()
            {{
                put("owner_id", ownerId + "");
                put("post_id", posts.getJSONObject(postNum).getLong("id") + "");
                put("count", MAX_CHUNK_SIZE + "");
                put("need_likes", 1 + "");
                put("v", API_VERSION + "");
            }});
            if (commentsStr.startsWith(ERROR_RETVAL) && i == 0) {
                return Optional.empty();
            }
            responseObj = (new JSONObject(commentsStr)).getJSONObject("response");
            if (responseObj.getLong("count") == 0) {
                return Optional.empty();
            }
            responseObj.getJSONArray("items").forEach((Object itm) -> {
                JSONObject item = (JSONObject) itm;
                item.remove("attachments");
                comments.put(item);
            });
        }
        return Optional.of(comments);
    }

    public Optional<JSONArray> GatherPosts(final long ownerId, int postsCount) throws IOException {
        String data;
        JSONObject responseObj;
        JSONArray postsArr = new JSONArray();
        for (int i = 0; postsCount > 0; i++) {
            final int chunkSize = Math.min(postsCount, MAX_CHUNK_SIZE);
            final int offset = i * MAX_CHUNK_SIZE;
            postsCount -= chunkSize;
            data = GatherData(GATHER_POSTS_METHOD, new HashMap<String, String>() {{
                put("owner_id", ownerId + "");
                put("count", chunkSize + "");
                put("offset", offset + "");
                put("v", API_VERSION + "");
            }});
            if (data.startsWith(ERROR_RETVAL) && i == 0) {
                return Optional.empty();
            }
            else if (!data.startsWith(ERROR_RETVAL)) {
                responseObj = (new JSONObject(data)).getJSONObject("response");
                if (responseObj.getLong("count") == 0) {
                    return Optional.empty();
                }
                responseObj.getJSONArray("items").forEach(postsArr::put);
            } else {
                return Optional.of(postsArr);
            }
        }
        return Optional.of(postsArr);
    }

    public String GatherData(String methodName, HashMap<String, String> args) throws IOException {
        String url = API_URL + methodName + "?";
        for (Map.Entry<String, String> arg : args.entrySet()) {
            url += arg.getKey() + "=" + arg.getValue() + "&";
        }
        url = url.replace("&$","");

        URL vk = new URL(url);
        URLConnection con = vk.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String data = "";
        String line;
        while ((line = in.readLine()) != null) {
            data += line;
        }
        in.close();
        return data;
    }

    private long GetNextPostId(final long ownerId)
    {

        VisitedPosts += 1;
        return 2977080;
    }

}
