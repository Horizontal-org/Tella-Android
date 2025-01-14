package org.horizontal.tella.mobile.data.rest;

import com.google.gson.annotations.SerializedName;


public class Response {
    // data property

    public static class Error {
        @SerializedName("code")
        private int code;

        @SerializedName("message")
        private String message;


        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
