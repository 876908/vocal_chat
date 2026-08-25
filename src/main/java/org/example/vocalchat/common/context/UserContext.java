package org.example.vocalchat.common.context;
//UserContext类用于保存当前线程的用户信息,使得user_id,token等不必层层传递，随时可以被调用
public final class UserContext {
    //ThreadLocal是一个泛型类，用于保存当前线程的变量，泛型类型即为保存的变量类型
    //用于保存当前线程的用户id（以对象的形式保存）
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    //用于保存当前线程的token
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
//及时清空，避免内存泄漏和线程复用后的安全问题
    public static void clear() {
        USER_ID.remove();
        TOKEN.remove();
    }
}
