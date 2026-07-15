package com.example.papersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.papersystem.entity.Paper;
import java.util.List;

public interface PaperService extends IService<Paper> {
    // 创建论文
    Paper createPaper(Paper paper, Long studentId);

    // 获取详情
    Paper getPaperById(Long id);

    // 获取列表
    List<Paper> getPapersByStudentId(Long studentId);

    // 保存内容
    void updatePaperContent(Long id, String content);

    // 删除论文
    void deletePaper(Long id);
}