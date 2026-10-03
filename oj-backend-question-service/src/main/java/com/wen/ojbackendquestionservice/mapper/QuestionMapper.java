package com.wen.ojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.ojbackendmodel.model.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
* @author a1472
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2026-09-16 22:33:46
* @Entity com.wen.oj.model.entity.Question
*/
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

}




