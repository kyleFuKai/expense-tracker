package com.xingzhewk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xingzhewk.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 查询账单导出数据（LEFT JOIN category 获取分类名称）
     */
    @Select("<script>" +
            "SELECT b.bill_time, b.type, c.name AS category_name, b.amount, b.remark, b.created_at " +
            "FROM bill b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.user_id = #{userId} " +
            "<if test='month != null'> AND DATE_FORMAT(b.bill_time, '%Y-%m') = #{month} </if>" +
            "<if test='type != null'> AND b.type = #{type} </if>" +
            "<if test='categoryId != null'> AND b.category_id = #{categoryId} </if>" +
            "<if test='keyword != null'> AND b.remark LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "<if test='startDate != null'> AND b.bill_time &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND b.bill_time &lt;= #{endDate} </if>" +
            "ORDER BY b.bill_time DESC" +
            "</script>")
    List<Map<String, Object>> selectForExport(
            @Param("userId") Long userId,
            @Param("month") String month,
            @Param("type") String type,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
