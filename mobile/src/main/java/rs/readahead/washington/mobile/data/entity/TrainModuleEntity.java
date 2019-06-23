package rs.readahead.washington.mobile.data.entity;

import com.google.gson.annotations.SerializedName;


public class TrainModuleEntity {
    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @SerializedName("url")
    public String url;

    @SerializedName("organization")
    public String organization;

    @SerializedName("type")
    public String type;

    @SerializedName("size")
    public long size;
}
