package com.example.chatapp.models;

public class ModelChat {

    String message, receiver, sender, timeStamp;
    boolean checked;

    public ModelChat() {

    }

    public ModelChat(String message, String receiver, String sender, String timeStamp, boolean checked) {
        this.message = message;
        this.receiver = receiver;
        this.sender = sender;
        this.timeStamp = timeStamp;
        this.checked = checked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean getChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
