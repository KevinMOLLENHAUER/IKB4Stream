package com.waves_rsp.ikb4stream.producer.score;

import com.waves_rsp.ikb4stream.core.model.Event;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import com.waves_rsp.ikb4stream.core.util.UtilManager;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.stream.Stream;

public class ScoreProcessorManager {
    private final Map<String,IScoreProcessor> scoreProcessors = new HashMap<>();

    public void processScore(Event event) {
        Objects.requireNonNull(event);

    }

    public void instanciate() throws IOException {
        String stringPath = PropertiesManager.getInstance().getProperty("scoreprocessormanager.path");
        try (Stream<Path> paths = Files.walk(Paths.get(stringPath))) {
            paths.forEach((Path filePath) -> {
                if (Files.isRegularFile(filePath)) {
                    URLClassLoader cl = UtilManager.getURLClassLoader(this.getClass().getClassLoader(), filePath);
                    Stream<JarEntry> e = UtilManager.getEntries(filePath);

                    e.filter(UtilManager::checkIsClassFile)
                            .map(UtilManager::getClassName)
                            .map(clazz -> UtilManager.loadClass(clazz, cl))
                            .filter(clazz -> UtilManager.implementInterface(clazz, IScoreProcessor.class))
                            .forEach(clazz -> {
                                IScoreProcessor scoreProcessor = (IScoreProcessor) UtilManager.newInstance(clazz);
                                scoreProcessors.put(scoreProcessor.getClass().getName(), scoreProcessor);
                            });
                }
            });
        }
    }

    public static void main(String[] args) throws IOException {
        ScoreProcessorManager producerManager = new ScoreProcessorManager();
        producerManager.instanciate();
    }
}
