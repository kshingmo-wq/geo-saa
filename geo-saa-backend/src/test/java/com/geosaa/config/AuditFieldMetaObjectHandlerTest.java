package com.geosaa.config;

import com.geosaa.common.BaseEntity;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计字段自动填充的回归测试。
 *
 * <p>背景：{@link BaseEntity} 声明了 {@code @TableField(fill = FieldFill.INSERT)}，
 * 但项目一度没有注册 {@link com.baomidou.mybatisplus.core.handlers.MetaObjectHandler}。
 * MyBatis-Plus 在缺少处理器时会把实体中的 null 当作<strong>显式值</strong>写入
 * INSERT 语句，于是建表 DDL 里的 {@code DEFAULT CURRENT_TIMESTAMP} 被架空，
 * 所有业务记录的 create_time / update_time 全为 NULL。
 *
 * <p>这个缺陷在 UI 上表现为：首页趋势图 X 轴退化成一片横杠（后端把 null 格式化成
 * {@code "-"}），让人误以为是图表组件坏了，实际是数据层问题。
 */
class AuditFieldMetaObjectHandlerTest {

    private MetaObject metaObjectOf(Object entity) {
        return SystemMetaObject.forObject(entity);
    }

    @Test
    @DisplayName("insert：create_time / update_time 为 null 时应填入当前时间")
    void insertFillPopulatesNullTimestamps() {
        BaseEntity entity = new BaseEntity();
        // 前置条件：实体初始时间字段应为 null，否则本用例测不到「自动填充」
        org.junit.jupiter.api.Assertions.assertNull(metaObjectOf(entity).getValue("createTime"));

        new AuditFieldMetaObjectHandler().insertFill(metaObjectOf(entity));

        LocalDateTime createTime = (LocalDateTime) metaObjectOf(entity).getValue("createTime");
        LocalDateTime updateTime = (LocalDateTime) metaObjectOf(entity).getValue("updateTime");

        assertNotNull(createTime, "createTime 应被自动填充");
        assertNotNull(updateTime, "updateTime 应被自动填充");
        // 填充值必须是「刚刚」，允许 5 秒时钟偏差
        assertTrue(Math.abs(System.currentTimeMillis() - createTime.atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()) < 5000,
                "填充的 createTime 应接近当前时间，实际: " + createTime);
        assertEquals(createTime, updateTime, "同一次插入两个时间戳应一致");
    }

    @Test
    @DisplayName("insert：调用方已显式指定时间时不应被覆盖（数据迁移/补录场景）")
    void insertFillKeepsExplicitValue() {
        BaseEntity entity = new BaseEntity();
        LocalDateTime explicit = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        // 模拟调用方 setCreateTime 后再触发填充
        entity.setCreateTime(explicit);

        new AuditFieldMetaObjectHandler().insertFill(metaObjectOf(entity));

        assertEquals(explicit, metaObjectOf(entity).getValue("createTime"),
                "已有显式值必须保留，不能被 now() 覆盖");
    }

    @Test
    @DisplayName("update：update_time 必须刷新为本次更新时刻（审计语义要求）")
    void updateFillRefreshesUpdateTime() {
        BaseEntity entity = new BaseEntity();
        LocalDateTime stale = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        entity.setUpdateTime(stale);

        new AuditFieldMetaObjectHandler().updateFill(metaObjectOf(entity));

        LocalDateTime after = (LocalDateTime) metaObjectOf(entity).getValue("updateTime");
        assertTrue(after.isAfter(stale),
                "update_time 应被刷新为更晚的时刻，实际: " + after);
    }

    @Test
    @DisplayName("update：create_time 属于插入字段，更新时不应被填充")
    void updateFillLeavesCreateTimeUntouched() {
        BaseEntity entity = new BaseEntity();

        new AuditFieldMetaObjectHandler().updateFill(metaObjectOf(entity));

        assertNull(metaObjectOf(entity).getValue("createTime"),
                "updateFill 只应动update_time，不应顺手填 create_time");
    }

    @Test
    @DisplayName("实体缺少审计字段时应静默跳过，不抛异常")
    void toleratesEntityWithoutAuditFields() {
        // MP 把本处理器注册为全局组件，会作用于所有 Mapper 上的实体；
        // 并非所有实体都继承 BaseEntity，缺字段时必须安全跳过。
        MetaObject plain = SystemMetaObject.forObject(new Object());

        new AuditFieldMetaObjectHandler().insertFill(plain);
        new AuditFieldMetaObjectHandler().updateFill(plain);
        // 无异常即通过
    }
}