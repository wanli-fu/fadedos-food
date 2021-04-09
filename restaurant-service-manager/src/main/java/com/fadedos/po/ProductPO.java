package com.fadedos.po;

import com.fadedos.enummeration.ProductStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class ProductPO {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Integer restaurantId;
    private ProductStatus status;
    private Date date;
}
