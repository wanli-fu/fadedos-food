package com.fadedos.po;

import com.fadedos.enummeration.RewardStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class RewardPO {
    private Integer id;
    private Integer orderId;
    private BigDecimal amount;
    private RewardStatus status;
    private Date date;
}
