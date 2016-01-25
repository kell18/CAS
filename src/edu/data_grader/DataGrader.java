package edu.data_grader;

import edu.json.JSONArray;
import edu.json.JSONObject;

import java.io.*;
import java.util.NoSuchElementException;

public class DataGrader {
    public static void main(String[] args) {
        VkGrader grader = new VkGrader();

        try {
            int lentachId = -29534144;
            JSONArray comments = grader.GatherComments(lentachId, 100).orElseThrow(
                    () -> new NoSuchElementException("No comments found"));
            JSONObject output = new JSONObject();
            output.put("comments", comments);
            output.put("count", comments.length());

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("res/data/LentachComments.json"), "utf-8"))) {
                writer.write(output.toString(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
