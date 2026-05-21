package com.expense.controller;

import com.expense.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * 提供 /api/health 接口，用于服务可用性探测。
 */
@RestController
public class HealthController {

    /**
     * 健康检查
     *
     * @return {status: "ok", timestamp: 当前时间}
     */
    @GetMapping("/api/health")
    public Result<?> health() {
        return Result.success(Map.of("status", "ok", "timestamp", LocalDateTime.now().toString()));
    }
}
