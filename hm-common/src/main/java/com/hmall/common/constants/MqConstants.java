package com.hmall.common.constants;

public interface MqConstants {
    String DELAY_EXCHANGE = "trade.delay.topic";
    String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    String DELAY_ORDER_ROUTING_KEY = "order.query";

    String ITEM_EXCHANGE_NAME = "item.change.topic";
    String ITEM_UP_KEY = "item.up";
    String ITEM_DOWN_KEY = "item.down";
}
