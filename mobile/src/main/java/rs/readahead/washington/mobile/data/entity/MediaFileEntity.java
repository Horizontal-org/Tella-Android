package rs.readahead.washington.mobile.data.entity;

import com.google.gson.annotations.SerializedName;


public final class MediaFileEntity {
    @SerializedName("id")
    public long id;

    @SerializedName("path")
    public String path;

    @SerializedName("uid")
    public String uid;

    @SerializedName("fileName")
    public String fileName;

    @SerializedName("metadata")
    public MetadataEntity metadata;

    @SerializedName("created")
    public long created;
}
