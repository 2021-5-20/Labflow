package dev.labflow.paper;

import dev.labflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "papers")
public class Paper extends BaseEntity {
    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String authors;

    @Column(columnDefinition = "text")
    private String url;

    @Column(columnDefinition = "text")
    private String tags;

    @Column(columnDefinition = "text")
    private String publication;

    private String publishedDate;

    @Column(columnDefinition = "text")
    private String keyClaims;

    @Column(columnDefinition = "text")
    private String notes;

    private UUID pdfFileId;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getPublication() {
        return publication;
    }

    public void setPublication(String publication) {
        this.publication = publication;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getKeyClaims() {
        return keyClaims;
    }

    public void setKeyClaims(String keyClaims) {
        this.keyClaims = keyClaims;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getPdfFileId() {
        return pdfFileId;
    }

    public void setPdfFileId(UUID pdfFileId) {
        this.pdfFileId = pdfFileId;
    }
}
