package org.juhrig.classroom.autograder.dto;

import com.google.api.services.classroom.model.*;

public class AutograderAttachment extends AutograderDTO {
    String id;
    String url;
    String altUrl;
    String title;
    AutograderAttachementType attachementType;

    public AutograderAttachment(){
        super(AutograderAttachment.class);
    }

    public AutograderAttachment(Attachment attachment){
        this();

        DriveFile driveFile = attachment.getDriveFile();
        Link link = attachment.getLink();
        Form form = attachment.getForm();
        YouTubeVideo youTubeVideo = attachment.getYouTubeVideo();

        if(driveFile != null){
            url = driveFile.getAlternateLink();
            altUrl = driveFile.getThumbnailUrl();
            title = driveFile.getTitle();
            id = driveFile.getId();
            attachementType = AutograderAttachementType.DRIVE_FILE;
        }
        else if(link != null){
            url = link.getUrl();
            altUrl = link.getThumbnailUrl();
            title = link.getTitle();
            id = link.getTitle();
            attachementType = AutograderAttachementType.LINK;
        }
        else if(form != null){
            url = form.getResponseUrl();
            altUrl = form.getThumbnailUrl();
            title = form.getTitle();
            id = form.getFormUrl();
            attachementType = AutograderAttachementType.FORM;
        }
        else if(youTubeVideo != null){
            url = youTubeVideo.getAlternateLink();
            id = youTubeVideo.getId();
            altUrl = youTubeVideo.getThumbnailUrl();
            title = youTubeVideo.getTitle();
            attachementType = AutograderAttachementType.YOUTUBE_LINK;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAltUrl() {
        return altUrl;
    }

    public void setAltUrl(String altUrl) {
        this.altUrl = altUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AutograderAttachementType getAttachementType() {
        return attachementType;
    }

    public void setAttachementType(AutograderAttachementType attachementType) {
        this.attachementType = attachementType;
    }
}
