package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
//    @Select("SELECT prev_hash FROM audit_logs ORDER BY id DESC LIMIT 1")
//    String getLatestHash();
}
