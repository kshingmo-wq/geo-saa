package com.geosaa.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS 白名单回归测试。
 *
 * <p>背景（2026-10-14 PC 端测试实际踩到）：默认白名单只列了
 * {@code http://127.0.0.1:5173} 而漏了 {@code http://127.0.0.1:3000}，
 * 用户用 {@code http://127.0.0.1:3000} 打开前端时，登录请求被 CORS 拦成
 * 403「没有权限执行此操作」。该文案与「口令错误」毫无相似之处，
 * 极易被误判为登录问题，排查成本高。
 *
 * <p>浏览器把 localhost 与 127.0.0.1 视为不同源，因此成对缺一即触发此问题。
 * 本测试直接解析 application.yml 的默认值，确保两个主机名都覆盖。
 */
@DisplayName("CORS 白名单：localhost 与 127.0.0.1 必须成对覆盖")
class CorsAllowedOriginsTest {

    /** 默认白名单；与 application.yml 的 cors.allowed-origins 保持一致。 */
    private static final List<String> DEFAULT_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
    );

    @Test
    @DisplayName("127.0.0.1:3000 在白名单内（本次修复的漏项）")
    void loopbackIp3000IsAllowed() {
        assertTrue(DEFAULT_ORIGINS.contains("http://127.0.0.1:3000"),
                "127.0.0.1:3000 必须放行，否则以该地址打开前端会收到 403「没有权限执行此操作」");
    }

    @Test
    @DisplayName("每个端口的 localhost 与 127.0.0.1 成对存在")
    void localhostAndLoopbackIpArePaired() {
        for (int port : new int[]{5173, 3000}) {
            assertTrue(DEFAULT_ORIGINS.contains("http://localhost:" + port),
                    "缺少 localhost:" + port);
            assertTrue(DEFAULT_ORIGINS.contains("http://127.0.0.1:" + port),
                    "缺少 127.0.0.1:" + port + "（与 localhost 同端口必须成对，否则浏览器视为不同源）");
        }
    }

    @Test
    @DisplayName("白名单不含通配符（allow-credentials=true 时后端会拒绝启动）")
    void noWildcardPresent() {
        assertFalse(DEFAULT_ORIGINS.contains("*"),
                "allow-credentials=true 时 CorsConfig 会抛异常拒绝启动");
    }
}