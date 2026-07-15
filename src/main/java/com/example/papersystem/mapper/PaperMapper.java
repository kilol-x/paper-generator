package com.example.papersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.papersystem.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论文 Mapper 接口
 * 继承 BaseMapper 后，你自动拥有了 insert, delete, update, selectById 等所有基础方法
 */
@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
    // 如果后续有复杂的自定义 SQL，可以在这里定义方法
}