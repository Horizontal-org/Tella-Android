package org.horizontal.tella.mobile.domain.entity.collect;

import java.util.List;

import org.horizontal.tella.mobile.data.http.HttpStatus;


public class OpenRosaResponse {
    private int statusCode;
    private List<Message> messages;


    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static class Message {
        private String nature;
        private String text;


        public String getNature() {
            return nature;
        }

        public void setNature(String nature) {
            this.nature = nature;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static final class StatusCode {
        public static final int UNUSED = HttpStatus.OK_200;
        public static final int FORM_RECEIVED = HttpStatus.CREATED_201;
        public static final int ACCEPTED = HttpStatus.ACCEPTED_202;
        public static final int NO_CONTENT = HttpStatus.NO_CONTENT_204;
        public static final int UNAUTHORIZED = HttpStatus.UNAUTHORIZED_401;
        public static final int FORBIDDEN = HttpStatus.FORBIDDEN_403;
        public static final int REQUEST_TOO_LARGE = HttpStatus.PAYLOAD_TOO_LARGE_413;
        public static final int INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR_500;
    }
}
