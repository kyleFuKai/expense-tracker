package com.xingzhewk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 修改密码请求参数
 */
@Data
public class ChangePasswordDTO {

    /** 原密码（前端传 old_password） */
    @JsonProperty("old_password")
    private String oldPassword;

    /** 新密码（前端传 new_password） */
    @JsonProperty("new_password")
    private String newPassword;
}
