package net.jforum.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends JForumException {
    
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}