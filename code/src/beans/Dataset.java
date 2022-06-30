package beans;

import java.util.ArrayList;
import java.util.List;

public class Dataset {
    private int id;
    private String title;
    private String description;
    private String author;
    private String url;
//    private String other_url;
    private String license;
//    private String parentLabel;
    private String version;
    private String portal;
    private String source;
    private boolean isComplete;

    private List<Resource> resources;

    public Dataset() {
    }

    public Dataset(int id, String title, String description, String author, String url, String license, String version, String portal, String source) {
        this.id = id;
        this.title = title == null? "": title;
        this.description = description == null? "": description;
        this.author = author == null? "": author;
        this.url = url == null? "": url;
//        this.other_url = other_url;
        this.license = license == null? "": license;
//        this.parentLabel = parentLabel;
        this.version = version == null? "": version;
        this.portal = portal == null? "": portal;
        this.source = source == null? "": source;
    }

    public Dataset(int id, String title, String description, String author, String url, String license) {
        this.id = id;
        this.title = title == null? "": title;
        this.description = description == null? "": description;
        this.author = author == null? "": author;
        this.url = url == null? "": url;
        this.license = license == null? "": license;
    }

    public void addResource(Resource resource) {
        if (this.resources == null) {
            this.resources = new ArrayList<>();
        }
        this.resources.add(resource);
    }

    public boolean setIsComplete(boolean isComplete) {
        this.isComplete = isComplete;
        return true;
    }

    public boolean getIsComplete() {
        return isComplete;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title == null? "": title;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description == null? "": description;
    }

    public String getDescription() {
        return description;
    }

    public void setAuthor(String author) {
        this.author = author == null? "": author;
    }

    public String getAuthor() {
        return author;
    }

    public void setUrl(String url) {
        this.url = url == null? "": url;
    }

    public String getUrl() {
        return url;
    }

    public void setLicense(String license) {
        this.license = license == null? "": license;
    }

    public String getLicense() {
        return license;
    }

    public void setVersion(String version) {
        this.version = version == null? "": version;
    }

    public String getVersion() {
        return version;
    }

    public void setPortal(String portal) {
        this.portal = portal == null? "": portal;
    }

    public String getPortal() {
        return portal;
    }

    public void setSource(String source) {
        this.source = source == null? "": source;
    }

    public String getSource() {
        return source;
    }

    public List<Resource> getResources() {
        return resources;
    }
}
