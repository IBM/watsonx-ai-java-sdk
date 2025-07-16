/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.image_analyzer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        List<Path> paths;

        if (args.length == 0)
            paths = List.of(Paths.get(App.class.getClassLoader().getResource("alien.jpg").toURI()));
        else
            paths = Stream.of(args).map(Path::of).toList();

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

        AuthenticationProvider authProvider = IAMAuthenticator.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

        ChatService chatService = ChatService.builder()
            .authenticationProvider(authProvider)
            .projectId(projectId)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .url(url)
            .build();

        for (Path path : paths) {

            var bytes = Files.readAllBytes(path);
            var mimeType = Files.probeContentType(path);
            var base64Data = Base64.getEncoder().encodeToString(bytes);

            var messages = List.<ChatMessage>of(
                SystemMessage
                    .of("You are a helpful assistant. Your task is to provide the user with a description of the image."),
                UserMessage.of(ImageContent.of(mimeType, base64Data))
            );

            var imageDescription = chatService.chat(messages).toText();

            System.out.println("""
                ----------------------------------------------------
                Filename: %s
                Description: %s
                ----------------------------------------------------"""
                .formatted(path.getFileName(), imageDescription));
        }
    }
}
