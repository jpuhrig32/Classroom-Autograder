package org.juhrig.classroom.autograder.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class InitializationParametersImpl implements InitializationParameters{

    private Properties properties;
    private final Path resourceDirRoot;
    private static final Logger LOG = LoggerFactory.getLogger(InitializationParametersImpl.class);

    public InitializationParametersImpl(String propertiesFilePath){
        properties = new Properties();
        resourceDirRoot = Paths.get(propertiesFilePath).toAbsolutePath().getParent();
        try {
            properties.load(new FileReader(propertiesFilePath));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key, null);
    }

    @Override
    public String getPropertyOrDefault(String key, String devaultValue) {
       return properties.getProperty(key, devaultValue);
    }

    @Override
    public Path getResourcesPath() {
        return resourceDirRoot;
    }
}
