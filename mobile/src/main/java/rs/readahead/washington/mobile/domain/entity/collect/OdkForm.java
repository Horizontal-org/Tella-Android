package rs.readahead.washington.mobile.domain.entity.collect;


public class OdkForm {
    private String formID;
    private String name;
    private String version;
    private String hash;
    private String downloadUrl;
    private String descriptionText;
    private String descriptionUrl;
    private String manifestUrl;


    public String getFormID() {
        return formID;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getHash() {
        return hash;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public void setFormID(String formID) {
        this.formID = formID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public String getDescriptionUrl() {
        return descriptionUrl;
    }

    public void setDescriptionUrl(String descriptionUrl) {
        this.descriptionUrl = descriptionUrl;
    }
}
