package com.ime.gbt32960.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Qingxi
 * 应答标志
 */

@AllArgsConstructor
@Getter
public enum ResponseTag {

    // 成功；接收到的消息正确
    SUCCESS((byte) 0x01),
    // 错误；设置未成功
    FAILED((byte) 0x02),
    // VIN重复；VIN重复错误
    VIN_DUP((byte) 0x03),
    // 命令；表示数据包为命令包，而非应答包
    COMMAND((byte) 0xFE);

    private final byte value;


    public static ResponseTag valueOf(byte type) {
        for (ResponseTag t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown message type : " + type);
    }

}
