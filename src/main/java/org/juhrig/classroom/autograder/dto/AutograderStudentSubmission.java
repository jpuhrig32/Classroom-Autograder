package org.juhrig.classroom.autograder.dto;

import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.GradeHistory;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.SubmissionHistory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutograderStudentSubmission extends AutograderDTO{

    String altLink;
    Double assignedGrade;
    Boolean late;
    String courseWorkId;
    List<AutograderAttachment> attachmentList;

    public AutograderStudentSubmission(){
        super(AutograderStudentSubmission.class);
    }

    public AutograderStudentSubmission(StudentSubmission submission){
        this();
        courseWorkId = submission.getCourseWorkId();
        altLink = submission.getAlternateLink();
        List<Attachment> detectedAttachments = submission.getAssignmentSubmission().getAttachments();
        if(detectedAttachments != null) {
            attachmentList = detectedAttachments.stream()
                    .map(gattachment -> new AutograderAttachment(gattachment))
                    .collect(Collectors.toList());
        }
        else{
            attachmentList = Collections.emptyList();
        }
        late = submission.getLate();

        assignedGrade = submission.getAssignedGrade();
        if(assignedGrade == null){
            assignedGrade = submission.getDraftGrade();
            if(assignedGrade == null){
                List<SubmissionHistory> history = submission.getSubmissionHistory();
                if(history != null){
                    for(SubmissionHistory historyItem: history){
                        if(historyItem != null){

                            GradeHistory gradeHistory = historyItem.getGradeHistory();
                            if(gradeHistory != null){
                                if(assignedGrade == null && gradeHistory.getPointsEarned() != null){
                                    assignedGrade = gradeHistory.getPointsEarned();
                                }
                                else if(gradeHistory.getPointsEarned() > assignedGrade){
                                    assignedGrade = gradeHistory.getPointsEarned();
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public String getAltLink() {
        return altLink;
    }

    public void setAltLink(String altLink) {
        this.altLink = altLink;
    }

    public Double getAssignedGrade() {
        return assignedGrade;
    }

    public void setAssignedGrade(Double assignedGrade) {
        this.assignedGrade = assignedGrade;
    }

    public Boolean getLate() {
        return late;
    }

    public void setLate(Boolean late) {
        this.late = late;
    }

    public String getCourseWorkId() {
        return courseWorkId;
    }

    public void setCourseWorkId(String courseWorkId) {
        this.courseWorkId = courseWorkId;
    }

    public List<AutograderAttachment> getAttachmentList() {
        return attachmentList;
    }

    public void setAttachmentList(List<AutograderAttachment> attachmentList) {
        this.attachmentList = attachmentList;
    }
}
