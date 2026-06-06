ALTER TABLE `user_coupon`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1未使用 2已使用 3已过期 4已失效 5已占用）';

ALTER TABLE `coupon_template`
    MODIFY COLUMN `amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠金额/折扣倍率（折扣券示例：0.90 表示 9 折）';
