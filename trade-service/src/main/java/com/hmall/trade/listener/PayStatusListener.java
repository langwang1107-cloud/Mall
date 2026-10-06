package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.MutilDelayMessage;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.common.constants.MqConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayStatusListener {
    private final IOrderService orderService;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "mark.order.pay.queue",durable = "true"),
            exchange = @Exchange(value = "pay.topic",type = ExchangeTypes.TOPIC),
            key = "pay.success"))
    public void listenPaySuccess(Long orderId){
        log.info("订单支付成功收到订单id：{}",orderId);
        try {
            orderService.markOrderPaySuccess(orderId);
        } catch (Exception e) {
            log.error("处理订单成功出现异常：",e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DELAY_ORDER_QUEUE,durable = "true"),
            exchange = @Exchange(value = MqConstants.DELAY_EXCHANGE,type = ExchangeTypes.TOPIC,delayed = "true"),
            key = MqConstants.DELAY_ORDER_ROUTING_KEY))
    public void listenDelayPay(MutilDelayMessage<Long> msg){
        log.info("收到延迟消息：{}",msg);
        if(msg==null||msg.getData()==null){
            log.error("发了个什么垃圾消息？：{}",msg);
            return;
        }
        Order order = orderService.getById(msg.getData());
        if(order==null){
            log.error("没查到订单：{}",msg);
        }
        //查询支付状态
        PayOrderDTO payOrder = payClient.getPayOrderByBizOrderNo(msg.getData());
        if(payOrder==null){
            //没查到支付单
            log.error("没查到支付单：{}",msg);
            return;
        }
        //判断是否支付
        if(payOrder.getStatus()==3){
            //说明支付成功
            log.info("用户已经支付成功了，所以修改订单状态即可");
            orderService.markOrderPaySuccess(msg.getData());
        }else if(payOrder.getStatus()==2){
            //已经取消过了
            log.error("支付单已经取消过了");
            //TODO 取消订单状态
        }else{
            //目前还没支付
            if(msg.hasNext()){
                //判断是否还有时间
                //继续发
                log.info("还有时间，用户还没支付，继续发：{}",msg);
                Long nextTime = msg.popNextTime();
                rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, msg, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDelay(nextTime.intValue());
                        return message;
                    }
                });
            }else{
                //没时间
                log.info("没时间了，所以把订单和支付单都取消掉：{}",msg);
                //TODO 取消订单状态
            }
        }
    }
}
