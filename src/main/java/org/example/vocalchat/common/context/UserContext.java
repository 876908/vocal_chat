package org.example.vocalchat.common.context;

public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private UserContext() {}

    public static void set(String userId, String token) {
        USER_ID.set(userId);
        TOKEN.set(token);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static String getToken() {
        return TOKEN.get();
    }

    public static String requireUserId() {
        String userId = USER_ID.get();
        if (userId == null) {
            throw new org.example.vocalchat.common.exception.BaseException(
                    org.example.vocalchat.common.enums.ErrorEnum.TOKEN_MISSING);
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
        TOKEN.remove();
    }
}
