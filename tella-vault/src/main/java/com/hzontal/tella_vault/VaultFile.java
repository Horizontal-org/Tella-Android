package com.hzontal.tella_vault;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class VaultFile  implements Serializable {
    public String id; // generated uuid
    public Type type;
    public String hash;
    public String path;
    public String mimeType = null;
    public String name;
    public long size;
    public long created;
    public long duration;
    public boolean anonymous;
    public Metadata metadata;
    public byte[] thumb;

    // todo: make this private and remove body
    public VaultFile() {
        this.id = UUID.randomUUID().toString();
    }

    protected VaultFile(BaseVaultFileBuilder<?, ?> builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.mimeType = builder.mimeType;
        this.thumb = builder.thumb;
        this.metadata = builder.metadata;
        this.anonymous = builder.anonymous;
        this.path = builder.path;
        this.hash = builder.hash;
        this.duration = builder.duration;
        this.size = builder.size;
    }

    public enum Type {
        UNKNOWN(0),
        FILE(1),
        DIRECTORY(2);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type fromValue(int value) {
            for (Type v: Type.values()) {
                if (v.getValue() == value) return v;
            }

            return UNKNOWN;
        }
    }
}
