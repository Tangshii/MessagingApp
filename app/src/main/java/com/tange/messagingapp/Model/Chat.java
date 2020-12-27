package com.tange.messagingapp.Model;

public class Chat {

    private String sender;
    private String receiver;
    private String message;
    private boolean isseen;
    private String type;
    private String prevTranslate;




    public Chat(String sender, String receiver, String message, boolean isseen, String prevTranslate) {

        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.isseen = isseen;

        this.type = type;

        this.prevTranslate = prevTranslate;
    }

    public Chat() {
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isIsseen() {
        return isseen;
    }

    public void setIsseen(boolean isseen) {
        this.isseen = isseen;
    }
    
      public String getType() {		     
         return type;		         
     }		     

 
      public void setType(String type) {		     
         this.type = type;		        
     }
                                        
    public String getPrevTranslate() {
        return prevTranslate;
    }

    public void setPrevTranslate(String prevTranslate) {
        this.prevTranslate = prevTranslate;
    }
}
