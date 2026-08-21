package org.horizontal.tella.mobile.data.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class FormMediaFileRegisterEntity {
    @SerializedName("attachments")
    public List<MediaFileEntity> attachments;
}
