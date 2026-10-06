package com.hmall.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MutilDelayMessage<T> implements Serializable {
    private T data;
    private List<Long> times;

    /**
     * 时间list里还有没有时间
     * @return true-有东西，false-没有
     */
    public boolean hasNext(){
        return !times.isEmpty();
    }

    /**
     * 弹出最新的时间
     * @return
     */
    public Long popNextTime(){
        return times.remove(0);
    }
}
