package com.knowledge.lock.exception;

public class LockAcquisitionException extends RuntimeException {

    public enum Reason {
        TIMEOUT,
        INTERRUPTED
    }

    private final Reason reason;

    private LockAcquisitionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private LockAcquisitionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public static LockAcquisitionException timeout(String message) {
        return new LockAcquisitionException(Reason.TIMEOUT, message);
    }

    public static LockAcquisitionException interrupted(String message, InterruptedException cause) {
        return new LockAcquisitionException(Reason.INTERRUPTED, message, cause);
    }

    public Reason getReason() {
        return reason;
    }
}
