package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.dto.CreateTagDTO;
import com.xingzhewk.dto.UpdateTagDTO;
import com.xingzhewk.vo.BillTagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface BillTagService {

    /**
     * 获取用户所有标签
     *
     * @param userId 用户 ID
     * @return 标签列表（含关联账单数）
     */
    Result<List<BillTagVO>> list(Long userId);

    /**
     * 创建标签
     *
     * @param userId 用户 ID
     * @param dto    创建参数
     * @return {id: 新标签ID}
     */
    Result<?> create(Long userId, CreateTagDTO dto);

    /**
     * 修改标签名
     *
     * @param userId 用户 ID
     * @param id     标签 ID
     * @param dto    更新参数
     * @return 空，code=404 表示标签不存在
     */
    Result<Void> update(Long userId, Long id, UpdateTagDTO dto);

    /**
     * 删除标签（级联删除关联）
     *
     * @param userId 用户 ID
     * @param id     标签 ID
     * @return 空，code=404 表示标签不存在
     */
    Result<Void> delete(Long userId, Long id);
}
