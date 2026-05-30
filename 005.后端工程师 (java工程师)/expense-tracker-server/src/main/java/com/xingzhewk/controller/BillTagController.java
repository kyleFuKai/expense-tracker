package com.xingzhewk.controller;

import com.xingzhewk.common.Result;
import com.xingzhewk.dto.CreateTagDTO;
import com.xingzhewk.dto.UpdateTagDTO;
import com.xingzhewk.service.BillTagService;
import com.xingzhewk.vo.BillTagVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 *
 * 处理标签的 CRUD 接口。所有接口均需 JWT 认证。
 * 路由前缀: /finance/tags
 */
@RestController
@RequestMapping("/finance/tags")
public class BillTagController {

    private final BillTagService billTagService;

    public BillTagController(BillTagService billTagService) {
        this.billTagService = billTagService;
    }

    /**
     * 获取用户所有标签
     *
     * @return [{id, name, bill_count}, ...]
     */
    @GetMapping
    public Result<List<BillTagVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return billTagService.list(userId);
    }

    /**
     * 创建标签
     *
     * @param dto 创建参数 {name: string}
     * @return {id: 新标签ID}
     */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @Valid @RequestBody CreateTagDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return billTagService.create(userId, dto);
    }

    /**
     * 修改标签名
     *
     * @param id  标签 ID
     * @param dto 更新参数 {name: string}
     * @return 空，code=404 表示标签不存在
     */
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody UpdateTagDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return billTagService.update(userId, id, dto);
    }

    /**
     * 删除标签（级联删除关联）
     *
     * @param id 标签 ID
     * @return 空，code=404 表示标签不存在
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return billTagService.delete(userId, id);
    }
}
