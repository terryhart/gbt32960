package com.ime.gbt32960.codec;

import com.ime.iov.gbt32960.PlatformMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Qingxi
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseMessage {
    private String vin;
    private PlatformMessage message;
}
