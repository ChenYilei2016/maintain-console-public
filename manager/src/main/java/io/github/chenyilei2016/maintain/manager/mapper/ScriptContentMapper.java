package io.github.chenyilei2016.maintain.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脚本内容Mapper接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface ScriptContentMapper extends BaseMapper<Script> {
}