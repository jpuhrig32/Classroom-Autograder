package org.juhrig.classroom.autograder.dto;

import com.google.api.services.classroom.model.CourseWork;

public class AutograderAssignment extends AutograderDTO {
    String title;
    String id;
    String workType;
    Double points;
    String workFolderId;
    String altWorkFolderLink;
    String shortTitle;
    String courseworkId;

    public AutograderAssignment(){super(AutograderAssignment.class);}
    public AutograderAssignment(CourseWork courseWork){
        this(courseWork, 0.0);
    }

    public AutograderAssignment(CourseWork courseWork, Double pointValueIfNotAssigned){
        super(AutograderAssignment.class);
        title = courseWork.getTitle();
        points = courseWork.getMaxPoints();
        if(points == null){
            points = pointValueIfNotAssigned;
        }
        id = courseWork.getId();
        workType = courseWork.getWorkType();
        workFolderId = courseWork.getAssignment().getStudentWorkFolder().getId();
        altWorkFolderLink = courseWork.getAssignment().getStudentWorkFolder().getAlternateLink();
        courseworkId = courseWork.getId();
        shortTitle = title;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getWorkType() {
        return workType;
    }

    public Double getPoints() {
        return points;
    }

    public String getWorkFolderId() {
        return workFolderId;
    }

    public String getAltWorkFolderLink() {
        return altWorkFolderLink;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public void setPoints(Double points) {
        this.points = points;
    }

    public void setWorkFolderId(String workFolderId) {
        this.workFolderId = workFolderId;
    }

    public void setAltWorkFolderLink(String altWorkFolderLink) {
        this.altWorkFolderLink = altWorkFolderLink;
    }

    public String getCourseworkId() {
        return courseworkId;
    }

    public void setCourseworkId(String courseworkId) {
        this.courseworkId = courseworkId;
    }


}
