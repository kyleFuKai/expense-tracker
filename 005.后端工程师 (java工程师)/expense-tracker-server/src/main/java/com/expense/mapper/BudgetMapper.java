package com.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.expense.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
