package dev.gungoguma.verify.server;

public final class OAuthCallbackResult {
    private final boolean success;
    private final String title;
    private final String message;

    private OAuthCallbackResult(boolean success, String title, String message) {
        this.success = success;
        this.title = title;
        this.message = message;
    }

    public static OAuthCallbackResult success(String message) {
        return new OAuthCallbackResult(true, "Verification Request Confirmed", message);
    }

    public static OAuthCallbackResult failure(String message) {
        return new OAuthCallbackResult(false, "Verification Failed", message);
    }

    public boolean success() {
        return success;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }
}
