package com.hzontal.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class FormVaultFileRegisterEntity {
    @SerializedName("attachments")
    public List<VaultFileEntity> attachments;
}
