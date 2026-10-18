package com.geosaa.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充。
 *
 * <p>{@link com.geosaa.common.BaseEntity} 用 {@code @TableField(fill = FieldFill.INSERT)}
 * 声明了 createTime / updateTime 由框架填充，但 MyBatis-Plus 只有在容器里存在
 * {@link MetaObjectHandler} 时才会执行填充。没有它，实体里的 null 会被当成
 * <strong>显式值</strong>写进 INSERT 语句，从而覆盖建表 DDL 里的
 * {@code DEFAULT CURRENT_TIMESTAMP} —— 结果是所有业务记录的 create_time 全为 NULL。
 *
 * <p>实际影响：首页趋势图（statistics/dashboard）以 create_time 作 X 轴，
 * 取不到值时后端回填 {@code "-"}，图表 X 轴就成了一排横杠，看不出任何时间维度。
 *
 * <p>实现说明：这里用「显式判空 + setFieldValByName」而不是 MP 提供的
 * {@code strictInsertFill}。后者内部依赖 {@code TableInfoHelper} 的全局注册表，
 * 只有在 MyBatis 完整初始化后才可用；本类的语义（仅当调用方未指定时才填）
 * 用三行判空即可达成，也让本类能在纯单元测试中被直接验证。
 *
 * <p>只补「框架该填但没填」的字段：已有值一律保留，避免覆盖调用方显式指定的
 * 时间（例如数据迁移、离线补录场景）。
 */
@Component
public class AuditFieldMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        fillIfAbsent(metaObject, CREATE_TIME, now);
        fillIfAbsent(metaObject, UPDATE_TIME, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // update_time 语义上就是「本次更新的时刻」，即便调用方传了旧值也应刷新，
        // 否则审计信息会失真。create_time 属于插入字段，此处不动。
        if (metaObject.hasSetter(UPDATE_TIME)) {
            setFieldValByName(UPDATE_TIME, LocalDateTime.now(), metaObject);
        }
    }

    /**
     * 仅当字段存在且当前值为 null 时写入。
     *
     * <p>用 {@code hasSetter} 兜住「实体未继承 BaseEntity」的情况：MP 会把本类
     * 作为全局处理器注册到所有 Mapper 上，并非所有实体都有这两个字段。
     */
    private void fillIfAbsent(MetaObject metaObject, String field, LocalDateTime value) {
        if (metaObject.hasSetter(field) && getFieldValByName(field, metaObject) == null) {
            setFieldValByName(field, value, metaObject);
        }
    }
}