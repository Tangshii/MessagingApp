package com.hike.messagingapp.Model;

public class User {

    private String id;
    private String username;
    private String imageURL;
    private String isGoogleSignUp;

    public User(String id, String username, String imageURL,String isGoogleSignUp) {
        this.id = id;
        this.username = username;
        this.imageURL = imageURL;
        this.isGoogleSignUp = isGoogleSignUp;
    }

    public User(){

    }



    public void setIsGoogleSignUp(String isGoogleSignUp) {
        this.isGoogleSignUp = isGoogleSignUp;
    }
    public String getIsGoogleSignUp() {
        return isGoogleSignUp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
