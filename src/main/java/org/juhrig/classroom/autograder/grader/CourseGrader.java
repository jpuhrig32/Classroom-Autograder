package org.juhrig.classroom.autograder.grader;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.juhrig.classroom.autograder.dto.AutograderAssignment;
import org.juhrig.classroom.autograder.dto.AutograderAttachment;
import org.juhrig.classroom.autograder.dto.AutograderStudent;
import org.juhrig.classroom.autograder.dto.AutograderStudentSubmission;
import org.juhrig.classroom.autograder.services.GoogleAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CourseGrader {

    private AutograderStudent student;
    private Map<String, AutograderAssignment> assignments;
    private List<AutograderStudentSubmission> submissions;

    private static final Logger LOG = LoggerFactory.getLogger(CourseGrader.class);

    public CourseGrader(AutograderStudent student, Map<String, AutograderAssignment> assignments, List<AutograderStudentSubmission> submissions){
        this.student = student;
        this.assignments = assignments;
        this.submissions = submissions;
    }

    public GradingResults gradeCourse(){
        Map<String, AutograderStudentSubmission> submissionMap = submissions.stream().collect(Collectors.toMap(AutograderStudentSubmission::getCourseWorkId, sub -> sub));

        Map<String, Double> gradesByAssignment = new LinkedHashMap<>();
        Map<String, Double> maxPointsByAssignment = new LinkedHashMap<>();
        Map<String, Boolean> assignmentTurnedIn = new LinkedHashMap<>();
        Map<String, Boolean> assignmentOnTime = new LinkedHashMap<>();
        Map<String, String> commentsByAssignment = new LinkedHashMap<>();

        for(String assignmentId : assignments.keySet()){
            AutograderAssignment assignment = assignments.get(assignmentId);
            AutograderStudentSubmission studentSubmission = submissionMap.get(assignment.getId());
            String assignmentName = assignment.getShortTitle();
            Double points = 0.0;
            Double maxPoints = assignment.getPoints();
            Boolean turnedIn = false;
            Boolean onTime = false;
            String comments = "";

            if(studentSubmission != null){
                if(!assignmentName.contains("Counseling")){
                    turnedIn = true;
                    points = studentSubmission.getAssignedGrade();
                    if(points == null || (points != null && (!Double.isFinite(points)|| points < 1.0)) ){
                        if(turnedIn){
                            points = assignment.getPoints();
                        }
                    }
                }
                else{
                    StringBuilder commentBuilder = new StringBuilder();
                    List<AttachmentCheckResults> counselingAttachmentResults = studentSubmission.getAttachmentList()
                            .stream()
                            .map(attachment -> checkAttachment(attachment))
                            .collect(Collectors.toList());
                    points = 0.0;
                    int foundAttachmentCount = counselingAttachmentResults.stream().filter(ca -> ca.isAttachmentFound()).collect(Collectors.toList()).size();
                    if(foundAttachmentCount == 0){
                        commentBuilder.append("No attachments found");
                    }
                    else if(foundAttachmentCount < 3){
                        commentBuilder.append("Missing attachments - only found " + Integer.toString(foundAttachmentCount));
                        points = maxPoints / 3.0 * foundAttachmentCount;
                    }
                    else{
                        commentBuilder.append("Found at least 3 attachments.");
                        points = maxPoints;
                    }
                    for(AttachmentCheckResults result: counselingAttachmentResults){
                        commentBuilder.append(",");
                        commentBuilder.append("Attachment id: " + result.getAttachment().getTitle());
                        commentBuilder.append(" ");
                        commentBuilder.append(result.getComments());
                    }
                    comments = commentBuilder.toString();
                }
                onTime = studentSubmission.getLate() != null ? !studentSubmission.getLate() : true;

            }
            else{
                comments = "No submission found";
            }
            gradesByAssignment.put(assignmentName, points);
            maxPointsByAssignment.put(assignmentName, maxPoints);
            assignmentTurnedIn.put(assignmentName, turnedIn);
            assignmentOnTime.put(assignmentName, onTime);
            commentsByAssignment.put(assignmentName, comments);
        }

        return new GradingResults(
                student,
                gradesByAssignment,
                maxPointsByAssignment,
                assignmentTurnedIn,
                assignmentOnTime,
                commentsByAssignment
        );
    }

    private AttachmentCheckResults checkAttachment(AutograderAttachment attachment){
        switch(attachment.getAttachementType()){
            case FORM:
                return new AttachmentCheckResults(false, "Google Form attachments are not supported", attachment);
            case LINK:
            case YOUTUBE_LINK:
                return checkLink(attachment.getUrl(), attachment);
            case DRIVE_FILE:
                return checkDriveFile(attachment, attachment);
        }
        return new AttachmentCheckResults(false, "Unknown attachment type", attachment);
    }

    private AttachmentCheckResults checkDriveFile(AutograderAttachment toCheck, AutograderAttachment attachment){
        Drive driveService = GoogleAuthenticationService.getDriveService();
        if(driveService == null){
            return new AttachmentCheckResults(false, "Could not connect to Google Drive service", attachment);
        }
        String id = toCheck.getId();
        try {
            Drive.Files.Get fileRequest = driveService.files().get(id);
            File requestResult = null;
            requestResult = fileRequest.execute();
            if(requestResult == null){
                return new AttachmentCheckResults(false, "Attachment not found", attachment);
            }
            else{
                String mimeType = requestResult.getMimeType();
                Long fileSize = requestResult.getSize();
                if((fileSize != null && fileSize < 512) || (mimeType == null)){
                    return new AttachmentCheckResults(false, "File below min size of 512 bytes", attachment);
                }
                else{
                    return new AttachmentCheckResults(true, "File OK", attachment);
                }
            }
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            return new AttachmentCheckResults(false, "An error occurred while checking the attachment", attachment);
        }

    }

    private AttachmentCheckResults checkLink(String linkUrl, AutograderAttachment attachment){
        try {
            URL url = new URL(linkUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            int status = conn.getResponseCode();
            InputStreamReader streamReader;
            if(status > 299){
                streamReader = new InputStreamReader(conn.getErrorStream());
                return new AttachmentCheckResults(false, conn.getResponseMessage(), attachment);
            }
            else{
                streamReader = new InputStreamReader(conn.getInputStream());
                return new AttachmentCheckResults(true, conn.getResponseMessage(), attachment);
            }
        }
        catch(MalformedURLException e){
            LOG.warn(e.getMessage(), e);
            return new AttachmentCheckResults(false, "URL was malformed or invalid", attachment);
        }
        catch(IOException e){
            LOG.warn(e.getMessage(), e);
            return new AttachmentCheckResults(false, "An error occurred when trying to connect to " + linkUrl, attachment);

        }


    }

    private class AttachmentCheckResults{
        boolean attachmentFound;
        String comments;
        AutograderAttachment attachment;

        public AttachmentCheckResults(boolean attachmentFound, String comments, AutograderAttachment attachment){
            this.attachmentFound = attachmentFound;
            this.comments = comments;
            this.attachment = attachment;
        }

        public boolean isAttachmentFound() {
            return attachmentFound;
        }

        public String getComments() {
            return comments;
        }

        public AutograderAttachment getAttachment() {
            return attachment;
        }
    }



}
