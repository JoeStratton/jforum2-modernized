package net.jforum.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends JForumException {
    
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}