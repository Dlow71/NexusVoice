package com.nexusvoice.infrastructure.context;

/**
 * 开发者API调用上下文
 * 通过ThreadLocal在一次请求内传递 developerApiKeyId 与 authType
 */
public class DeveloperApiContext {

    private static final ThreadLocal<Long> DEV_API_KEY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> AUTH_TYPE = new ThreadLocal<>();

    public static void setDeveloperApiKeyId(Long id) {
        DEV_API_KEY_ID.set(id);
    }

    public static Long getDeveloperApiKeyId() {
        return DEV_API_KEY_ID.get();
    }

    public static void setAuthType(String authType) {
        AUTH_TYPE.set(authType);
    }

    public static String getAuthType() {
        return AUTH_TYPE.get();
    }

    public static void clear() {
        DEV_API_KEY_ID.remove();
        AUTH_TYPE.remove();
    }
}

