package com.codecollab.executionservice.service;

import com.codecollab.executionservice.dto.CodeExecutionResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final DockerClient dockerClient;

    public CodeExecutionResponse executeCode(String code, String language, String stdin) throws Exception {
        Path tempDir = Files.createTempDirectory("codecollab-" + UUID.randomUUID());

        File inputFile = new File(tempDir.toFile(), "input.txt");
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(stdin != null ? stdin : "");
        }

        // --- THIS IS THE NEW MULTI-LANGUAGE LOGIC ---
        String sourceFileName;
        String dockerfileContent;

        switch (language.toLowerCase()) {
            case "java":
                sourceFileName = "Main.java";
                dockerfileContent = """
                    FROM openjdk:17-slim
                    WORKDIR /app
                    COPY . .
                    RUN javac Main.java
                    CMD sh -c "java Main < input.txt"
                    """;
                break;
            case "python":
                sourceFileName = "main.py";
                dockerfileContent = """
                    FROM python:3.11-slim
                    WORKDIR /app
                    COPY . .
                    CMD sh -c "python main.py < input.txt"
                    """;
                break;
            case "javascript":
                sourceFileName = "index.js";
                dockerfileContent = """
                    FROM node:18-slim
                    WORKDIR /app
                    COPY . .
                    CMD sh -c "node index.js < input.txt"
                    """;
                break;
            default:
                FileUtils.deleteDirectory(tempDir.toFile());
                throw new IllegalArgumentException("Unsupported language: " + language);
        }

        File sourceFile = new File(tempDir.toFile(), sourceFileName);
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(code);
        }

        File dockerfile = new File(tempDir.toFile(), "Dockerfile");
        try (FileWriter writer = new FileWriter(dockerfile)) {
            writer.write(dockerfileContent);
        }

        // The rest of the Docker execution logic is the same and requires no changes
        String imageId = null;
        String containerId = null;
        StringBuilder buildLog = new StringBuilder();
        try {
            BuildImageResultCallback callback = new BuildImageResultCallback(buildLog);
            dockerClient.buildImageCmd(tempDir.toFile()).exec(callback);
            boolean completed = callback.awaitCompletion(60, TimeUnit.SECONDS);
            if (!completed) return new CodeExecutionResponse("", "Docker image build timed out.");
            imageId = callback.getImageId();
            if (imageId == null) return new CodeExecutionResponse("", buildLog.toString());

            HostConfig hostConfig = new HostConfig().withMemory(128 * 1024 * 1024L).withCpuCount(1L);
            CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageId).withHostConfig(hostConfig).exec();
            containerId = containerResponse.getId();
            dockerClient.startContainerCmd(containerId).exec();
            dockerClient.waitContainerCmd(containerId).start().awaitStatusCode(10, TimeUnit.SECONDS);

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            LogCallback logCallback = new LogCallback(output, error);
            dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).exec(logCallback).awaitCompletion(5, TimeUnit.SECONDS);
            return new CodeExecutionResponse(output.toString(), error.toString());

        } finally {
            cleanup(containerId, imageId, tempDir);
        }
    }

    private void cleanup(String containerId, String imageId, Path tempDir) {
        if (containerId != null) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                System.err.println("Failed to remove container: " + containerId);
            }
        }
        if (imageId != null) {
            try {
                dockerClient.removeImageCmd(imageId).withForce(true).exec();
            } catch (Exception e) {
                System.err.println("Failed to remove image: " + imageId);
            }
        }
        try {
            FileUtils.deleteDirectory(tempDir.toFile());
        } catch (IOException e) {
            System.err.println("Failed to delete temp directory: " + tempDir);
        }
    }

    // Helper class for RUNTIME log capturing
    private static class LogCallback extends ResultCallback.Adapter<Frame> {
        private final StringBuilder output;
        private final StringBuilder error;
        public LogCallback(StringBuilder output, StringBuilder error) { this.output = output; this.error = error; }
        @Override
        public void onNext(Frame item) {
            if (item.getStreamType() == com.github.dockerjava.api.model.StreamType.STDOUT) {
                output.append(new String(item.getPayload()));
            } else {
                error.append(new String(item.getPayload()));
            }
        }
    }

    // Corrected Helper class for BUILD log capturing
    private static class BuildImageResultCallback extends ResultCallback.Adapter<BuildResponseItem> {
        private final StringBuilder buildLog;
        private String imageId;

        public BuildImageResultCallback(StringBuilder buildLog) {
            this.buildLog = buildLog;
        }

        @Override
        public void onNext(BuildResponseItem item) {
            if (item.getStream() != null) {
                buildLog.append(item.getStream());
            }
            if (item.isBuildSuccessIndicated()) {
                this.imageId = item.getImageId();
            }
        }

        public String getImageId() {
            return this.imageId;
        }
    }
}