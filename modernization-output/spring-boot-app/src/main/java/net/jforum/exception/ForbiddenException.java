package net.jforum.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends JForumException {
    
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}