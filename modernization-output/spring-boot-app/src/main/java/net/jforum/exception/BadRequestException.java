package net.jforum.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends JForumException {
    
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}