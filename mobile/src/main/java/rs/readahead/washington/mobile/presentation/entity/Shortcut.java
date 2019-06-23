package rs.readahead.washington.mobile.presentation.entity;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;


public class Shortcut {
    public static Shortcut NONE = new Shortcut();

    @SerializedName("t")
    private ShortcutType type;

    @SerializedName("l")
    private String label;

    @SerializedName("p")
    private String parameter;


    private Shortcut() {
    }

    public Shortcut(ShortcutType type, String label) {
        this(type, label, null);
    }

    public Shortcut(ShortcutType type, String label, String parameter) {
        this.type = type;
        this.label = label;
        this.parameter = parameter;
    }

    public ShortcutType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getParameter() {
        return parameter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Shortcut)) return false;

        Shortcut that = (Shortcut) obj;

        return (this.type == that.type && TextUtils.equals(this.parameter, that.parameter));
    }
}