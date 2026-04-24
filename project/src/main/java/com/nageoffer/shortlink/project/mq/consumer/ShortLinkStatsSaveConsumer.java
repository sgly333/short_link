/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 短链接监控状态保存消息队列消费者
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Component
@RequiredArgsConstructor
// 指定了消费的topic
@RocketMQMessageListener(
        topic = "${rocketmq.producer.topic}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class ShortLinkStatsSaveConsumer implements RocketMQListener<Map<String, String>> {

    private static final String STATS_FLUSH_KEY_PREFIX = "short-link:stats:flush";

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Override
    public void onMessage(Map<String, String> producerMap) {
        String keys = producerMap.get("keys");
        if (!messageQueueIdempotentHandler.isMessageProcessed(keys)) {
            // 判断当前的这个消息流程是否执行完成
            if (messageQueueIdempotentHandler.isAccomplish(keys)) {
                return;
            }
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }
        try {
            ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            actualSaveShortLinkStats(statsRecord);
        } catch (Throwable ex) {
            log.error("记录短链接监控消费异常", ex);
            try {
                messageQueueIdempotentHandler.delMessageProcessed(keys);
            } catch (Throwable remoteEx) {
                log.error("删除幂等标识错误", remoteEx);
            }
            throw ex;
        }
        messageQueueIdempotentHandler.setAccomplish(keys);
    }

    public void actualSaveShortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        Date currentDate = statsRecord.getCurrentDate();
        int hour = DateUtil.hour(currentDate, true);
        Week week = DateUtil.dayOfWeekEnum(currentDate);
        int weekValue = week.getIso8601Value();
        String dateTag = DateUtil.formatDate(currentDate);
        String key = String.format("%s:%s:%s:%d:%d", STATS_FLUSH_KEY_PREFIX, DigestUtil.md5Hex(fullShortUrl), dateTag, hour, weekValue);
        stringRedisTemplate.opsForHash().putIfAbsent(key, "fullShortUrl", fullShortUrl);
        stringRedisTemplate.opsForHash().putIfAbsent(key, "date", dateTag);
        stringRedisTemplate.opsForHash().putIfAbsent(key, "hour", String.valueOf(hour));
        stringRedisTemplate.opsForHash().putIfAbsent(key, "weekday", String.valueOf(weekValue));
        stringRedisTemplate.opsForHash().increment(key, "pv", 1L);
        stringRedisTemplate.opsForHash().increment(key, "uv", statsRecord.getUvFirstFlag() ? 1L : 0L);
        stringRedisTemplate.opsForHash().increment(key, "uip", statsRecord.getUipFirstFlag() ? 1L : 0L);
    }
}

