package beans;

import java.util.ArrayList;
import java.util.List;

public class Resource {
    private int id;
    private String name;
    private String download;
    private String created;
    private String updated;
    private String notes;
    private String size;
    private List<String> tags;
    private String version;

    public Resource(int id, String name, String download, String created, String updated, String notes, String size, List<String> tags) {
        this.id = id;
        this.name = name == null? "": name;
        this.download = download == null? "": download;
        this.created = created == null? "": created;
        this.updated = updated == null? "": updated;
        this.notes = notes == null? "": notes;
        this.size = size == null? "": size;
        this.tags = tags;
    }

    public Resource(int id, String name, String download, String created, String updated, String notes, String size, List<String> tags, String version) {
        this.id = id;
        this.name = name == null? "": name;
        this.download = download == null? "": download;
        this.created = created == null? "": created;
        this.updated = updated == null? "": updated;
        this.notes = notes == null? "": notes;
        this.size = size == null? "": size;
        this.tags = tags;
        this.version = version == null? "": version;
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

    public String getVersion() {
        return version;
    }

//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setCreated(String created) {
//        this.created = created;
//    }
//
//    public void setUpdated(String updated) {
//        this.updated = updated;
//    }
//
//    public void setDownload(String download) {
//        this.download = download;
//    }
//
//    public void setTags(List<String> tags) {
//        this.tags = tags;
//    }
//
//    public void setNotes(String notes) {
//        this.notes = notes;
//    }
//
//    public void setSize(String size) {
//        this.size = size;
//    }
}
