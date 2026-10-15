package io.github.mahdiarharandi.articles.exception;
import org.springframework.http.HttpStatus;
public class AuthenticationFailedException extends ApiException {
    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Username or password is incorrect.");
    }
}
