package cn.chenyilei.maintain.manager.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.experimental.UtilityClass;

/**
 * @author chenyilei
 * @since 2025/07/31 20:22
 */
@UtilityClass
public class IdUtil {

    public String generateSnowFlakeId() {
        // 这里可以使用UUID或其他方式生成唯一ID
        return IdWorker.getIdStr();
    }
}
