package com.example.algorithm.constraint;

public final class ConstraintResult {

    private final boolean valid;
    private final String  reason;

    private ConstraintResult(boolean valid, String reason) {
        this.valid  = valid;
        this.reason = reason;
    }

    // Factory: constraint passed.
    public static ConstraintResult pass() {
        return new ConstraintResult(true, null);
    }

    // Factory: constraint failed with an explanation.
    public static ConstraintResult fail(String reason) {
        return new ConstraintResult(false, reason);
    }

    public boolean isValid()  { return valid;  }
    public String  getReason(){ return reason; }

    @Override
    public String toString() {
        return valid ? "PASS" : "FAIL(" + reason + ")";
    }
}

