package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;


public class Feedback implements Serializable {
    private String name;
    private String mail;
    private String message;


    public Feedback(String name, String mail, String message) {
        this.name = name;
        this.mail = mail;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return mail;
    }

    public void setEmail(String mail) {
        this.mail = mail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
