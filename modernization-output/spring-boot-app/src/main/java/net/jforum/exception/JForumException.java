package net.jforum.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JForumException extends RuntimeException {
    
    private final HttpStatus status;
    
    public JForumException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public JForumException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
}