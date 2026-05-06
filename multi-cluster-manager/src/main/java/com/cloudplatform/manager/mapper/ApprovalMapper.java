package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {

//    @Select("SELECT * FROM approvals WHERE task_id = #{taskId} AND action = 'pending' ORDER BY created_at DESC LIMIT 1")
//    Approval findPendingByTaskId(@Param("taskId") Long taskId);

//    @Select("SELECT * FROM approvals WHERE approver_id = #{approverId} AND action = 'pending' ORDER BY created_at ASC")
//    List<Approval> findPendingByApproverId(@Param("approverId") Long approverId);
//
//    @Select("SELECT * FROM approvals WHERE approver_id = #{approverId} AND action IN ('approved','rejected','expired') ORDER BY created_at DESC")
//    List<Approval> findHistoryByApproverId(@Param("approverId") Long approverId);

//    @Update("UPDATE approvals SET action = 'expired' WHERE action = 'pending' AND expires_at < NOW()")
//    int batchExpirePending();
}
