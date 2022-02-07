package org.juhrig.classroom.autograder.parameters;

import java.nio.file.Path;

public interface InitializationParameters{

    String getProperty(String key);
    String getPropertyOrDefault(String key, String devaultValue);
    Path getResourcesPath();
}
