/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.image_analyzer;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            List<Path> paths;

            if (args.length == 0)
                paths = List.of(Paths.get(App.class.getClassLoader().getResource("alien.jpg").toURI()));
            else
                paths = Stream.of(args).map(Path::of).toList();

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            ChatService chatService = ChatService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .timeout(Duration.ofSeconds(60))
                .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
                .baseUrl(url)
                .build();

            for (Path path : paths) {

                var imageDescription = chatService.chat(
                    UserMessage.of(
                        TextContent.of("Write a short description of the image"),
                        ImageContent.from(path)
                    )).extractContent();

                System.out.println("""
                    ----------------------------------------------------
                    Filename: %s
                    Description: %s
                    ----------------------------------------------------"""
                    .formatted(path.getFileName(), imageDescription));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
