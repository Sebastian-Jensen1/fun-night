package com.funnight.backend.exception;

public class SessionNotFoundException extends RuntimeException {
  public SessionNotFoundException(Long sessionId) {super("Session with ID " + sessionId + " does not exist.");}
}
