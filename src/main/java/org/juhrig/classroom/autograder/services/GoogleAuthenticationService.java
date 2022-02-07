package org.juhrig.classroom.autograder.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.juhrig.classroom.autograder.parameters.InitializationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleAuthenticationService {
    private static Drive driveService;
    private static Classroom classroomService;
    private static Credential loginCredentials;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = List.of(
            DriveScopes.DRIVE_READONLY,
            ClassroomScopes.CLASSROOM_COURSES,
            ClassroomScopes.CLASSROOM_ROSTERS,
            ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS,
            ClassroomScopes.CLASSROOM_COURSEWORKMATERIALS
    );
    private static String credentialsFilePath;
    private static String tokenStoragePath;
    private static GoogleClientSecrets clientSecrets;
    private static  String applicationName;
    private static final Logger LOG = LoggerFactory.getLogger(GoogleAuthenticationService.class);

    public static void initialize(InitializationParameters initializationParameters){
        credentialsFilePath = initializationParameters.getPropertyOrDefault("credentials.file", "credentials.json");
        tokenStoragePath = initializationParameters.getPropertyOrDefault("credentials.token.file", "tokens");
        applicationName = initializationParameters.getPropertyOrDefault("application.name", "Classroom Autograder");

        String credentialPath = initializationParameters.getResourcesPath().resolve(credentialsFilePath).toString();
        try {
            InputStream credentialInput = new FileInputStream(credentialPath);
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(credentialInput));
            createDriveService();
            createClassroomService();
        }
        catch(IOException e){
            LOG.error(e.getMessage(), e);
        }
    }

    public static Drive getDriveService(){
        if(driveService == null){
            createDriveService();
        }
        return driveService;
    }

    public static Classroom getClassroomService(){
        if(classroomService == null){
            createClassroomService();
        }
        return classroomService;
    }

    private static Credential getLoginCredentials(){
        if(loginCredentials == null){
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                loginCredentials = retrieveCredentials(HTTP_TRANSPORT);
            }
            catch(GeneralSecurityException | IOException e){
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
        return loginCredentials;
    }
    private static Credential retrieveCredentials(final NetHttpTransport HTTP_TRANSPORT) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    clientSecrets,
                    SCOPES
            )
                    .setDataStoreFactory(new FileDataStoreFactory(new File(tokenStoragePath)))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
        catch(IOException e){
            LOG.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    private static void createDriveService(){
        try {
            final NetHttpTransport HTTP_TRANSPORT= GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getLoginCredentials();
            if(credential == null){
                LOG.error("Could not obtain classroom credentails");
                return;
            }
            driveService = new Drive.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
    private static void createClassroomService(){
        try {
            final NetHttpTransport HTTP_TRANSPORT= GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getLoginCredentials();
            if(credential == null){
                LOG.error("Could not obtain classroom credentails");
                return;
            }
            classroomService = new Classroom.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

}
