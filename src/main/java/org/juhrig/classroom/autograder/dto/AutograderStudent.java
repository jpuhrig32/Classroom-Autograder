package org.juhrig.classroom.autograder.dto;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutograderStudent extends AutograderDTO{

    private static final Logger LOG = LoggerFactory.getLogger(AutograderStudent.class);
    String firstName;
    String lastName;
    String fullName;
    String userId;
    String email;
    List<AutograderStudentSubmission> studentSubmissions;

    public AutograderStudent(){
        super(AutograderStudent.class);
    }

    public AutograderStudent(Student student){
        this();
        userId = student.getUserId();
        UserProfile profile = student.getProfile();
        email = profile.getEmailAddress();
        Name studentName = profile.getName();
        firstName = studentName.getGivenName();
        lastName = studentName.getFamilyName();
        fullName = studentName.getFullName();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean loadStudentSubmissions(Classroom classroomService, String courseId){
        try {
            Classroom.Courses.CourseWork.StudentSubmissions.List submissionsRequest = classroomService.courses().courseWork().studentSubmissions().list(courseId, "-");
            submissionsRequest.setPageSize(100);
            submissionsRequest.setUserId(userId);
            studentSubmissions = new ArrayList<>();
            while(true){
               ListStudentSubmissionsResponse response = submissionsRequest.execute();
               List<StudentSubmission> responseSubs = response.getStudentSubmissions();
               List<AutograderStudentSubmission> responseSubmissionList = responseSubs.stream()
                       .map(sub -> new AutograderStudentSubmission(sub))
                       .collect(Collectors.toList());
               studentSubmissions.addAll(responseSubmissionList);
               if(response.getNextPageToken() != null){
                   submissionsRequest.setPageToken(response.getNextPageToken());
               }
               else{
                   break;
               }
            }
            return true;
        } catch (IOException e) {
            LOG.warn(this.fullName + " submission request: " + e.getMessage(), e);
        }
        return false;
    }

    public List<AutograderStudentSubmission> getStudentSubmissions() {
        return studentSubmissions;
    }
}
