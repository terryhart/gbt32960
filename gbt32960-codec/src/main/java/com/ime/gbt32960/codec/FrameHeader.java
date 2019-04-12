package com.ime.gbt32960.codec;

import io.netty.util.ByteProcessor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author ZhangSaiqiang
 *
 */

@Builder
@Getter
public class FrameHeader {

    public static final int HEADER_LENGTH = 22;

    private RequestType requestType;
    private ResponseTag responseTag;
    private String vin;
    private EncryptionType encryptionType;
    private int payloadLength;

    public static final class CheckCodeProcessor implements ByteProcessor {

        @Getter
        private byte checkCode = (byte) 0x00;

        @Override
        public boolean process(byte value) throws Exception {
            checkCode ^= value;
            return true;
        }
    }

}
