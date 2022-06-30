package beans;

import java.util.List;

public class DatasetResource {
    int id;
    String name;
    String download;
    String created;
    String updated;
    String notes;
    String size;
    List<String> tags;

    public DatasetResource() {
    }

    public DatasetResource(int id, String name, String download, String created, String updated, String notes, String size, List<String> tags) {
        this.id = id;
        this.name = name;
        this.download = download;
        this.created = created;
        this.updated = updated;
        this.tags = tags;
        this.notes = notes;
        this.size = size;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDownload() {
        return download;
    }

    public String getCreated() {
        return created;
    }

    public String getUpdated() {
        return updated;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getNotes() {
        return notes;
    }

    public String getSize() {
        return size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
