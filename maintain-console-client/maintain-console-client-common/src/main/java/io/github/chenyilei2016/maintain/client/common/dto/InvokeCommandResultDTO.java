package io.github.chenyilei2016.maintain.client.common.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author chenyilei
 * @since 2024/05/17 11:44
 */
@Data
@ToString(callSuper = true)
public class InvokeCommandResultDTO implements Serializable {

    private static final long serialVersionUID = 9002001097063515932L;

    protected long timestamp = System.currentTimeMillis();
}
