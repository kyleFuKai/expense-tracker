package com.xingzhewk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingzhewk.common.Result;
import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.dto.CreateTagDTO;
import com.xingzhewk.dto.UpdateTagDTO;
import com.xingzhewk.entity.BillTag;
import com.xingzhewk.entity.BillTagRel;
import com.xingzhewk.mapper.BillTagMapper;
import com.xingzhewk.mapper.BillTagRelMapper;
import com.xingzhewk.service.BillTagService;
import com.xingzhewk.vo.BillTagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillTagServiceImpl implements BillTagService {

    private final BillTagMapper billTagMapper;
    private final BillTagRelMapper billTagRelMapper;

    @Override
    public Result<List<BillTagVO>> list(Long userId) {
        List<BillTag> tags = billTagMapper.selectList(
                new LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getUserId, userId)
                        .orderByAsc(BillTag::getCreatedAt)
        );

        List<BillTagVO> vos = tags.stream().map(tag -> {
            BillTagVO vo = new BillTagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            long count = billTagRelMapper.selectCount(
                    new LambdaQueryWrapper<BillTagRel>()
                            .eq(BillTagRel::getTagId, tag.getId())
            );
            vo.setBillCount(count);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> create(Long userId, CreateTagDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException(400, "标签名不能为空");
        }
        if (dto.getName().length() > 16) {
            throw new BusinessException(400, "标签名不能超过16个字符");
        }

        long exists = billTagMapper.selectCount(
                new LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getUserId, userId)
                        .eq(BillTag::getName, dto.getName())
        );
        if (exists > 0) {
            throw new BusinessException(400, "该标签已存在");
        }

        BillTag tag = new BillTag();
        tag.setUserId(userId);
        tag.setName(dto.getName());

        billTagMapper.insert(tag);
        log.info("创建标签, tagId={}, userId={}, name={}", tag.getId(), userId, dto.getName());

        Map<String, Object> result = new HashMap<>();
        result.put("id", tag.getId());
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long userId, Long id, UpdateTagDTO dto) {
        BillTag existing = billTagMapper.selectOne(
                new LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getId, id)
                        .eq(BillTag::getUserId, userId)
        );
        if (existing == null) {
            throw new BusinessException(404, "标签不存在");
        }
        if (dto.getName() != null && dto.getName().length() > 16) {
            throw new BusinessException(400, "标签名不能超过16个字符");
        }

        long conflict = billTagMapper.selectCount(
                new LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getUserId, userId)
                        .eq(BillTag::getName, dto.getName())
                        .ne(BillTag::getId, id)
        );
        if (conflict > 0) {
            throw new BusinessException(400, "该标签已存在");
        }

        BillTag update = new BillTag();
        update.setId(id);
        update.setName(dto.getName());
        billTagMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long userId, Long id) {
        BillTag existing = billTagMapper.selectOne(
                new LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getId, id)
                        .eq(BillTag::getUserId, userId)
        );
        if (existing == null) {
            throw new BusinessException(404, "标签不存在");
        }

        billTagRelMapper.delete(
                new LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getTagId, id)
        );

        billTagMapper.deleteById(id);
        return Result.success();
    }
}
