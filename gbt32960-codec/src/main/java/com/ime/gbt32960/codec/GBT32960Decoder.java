package com.ime.gbt32960.codec;

import com.ime.iov.gbt32960.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static com.ime.gbt32960.codec.FrameHeader.HEADER_LENGTH;
import static com.ime.gbt32960.codec.GBT32960Message.*;

/**
 * @author Qingxi
 */

@Slf4j
public class GBT32960Decoder extends ReplayingDecoder<Void> {

    public GBT32960Decoder() {
        super();
    }

    private FrameHeader frameHeader;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("gbt32960 inbound");
        checkpoint();
        log.info("帧消息: {}", ByteBufUtil.hexDump(internalBuffer()));

        if (in.readShort() != START_SYMBOL) {
            in.skipBytes(actualReadableBytes());
            ctx.close();
            return;
        }

        int startIndex = in.readerIndex();
        frameHeader = decodeFrameHeader(in);

        int payloadLength = frameHeader.getPayloadLength();
        byte checkCode = checkCode(in, startIndex, HEADER_LENGTH + payloadLength);
        byte checkCodeInMsg = in.getByte(in.readerIndex() + payloadLength);

        if (checkCode != checkCodeInMsg) {
            in.skipBytes(payloadLength + 1);
            log.info("消息校验位验证失败: {} vs {}", String.format("%02X", checkCode),
                    String.format("%02X", checkCodeInMsg));
            return;
        }

        Object payload = decodePayload(in, frameHeader);
        GBT32960Message message = GBT32960Message.builder()
                .header(frameHeader)
                .payload(payload)
                .build();
        out.add(message);
        // 校验位
        in.skipBytes(1);
    }

    private Object decodePayload(ByteBuf in, FrameHeader header) {
        switch (header.getRequestType()) {

            case REAL_TIME:
            case REISSUE:
                ByteBuf fullBody = in.readRetainedSlice(header.getPayloadLength());
                RealTimeReport.Builder report = ReportDecoder.decodeFully(fullBody);
                fullBody.release();
                report.setReissue(header.getRequestType() == RequestType.REISSUE);
                RealTimeReport timeReport = report.build();
                log.info("实时信息：\n数据采集时间:{}\n{}", ZonedDateTime.ofInstant(Instant.ofEpochSecond(timeReport.getRecordTime()), ZONE_UTC8), timeReport);
                return timeReport;

            case LOGIN:
                LoginRequest loginRequest = decodeLogin(in);
                log.info("登入信息: \n数据采集时间:{}\n{}", ZonedDateTime.ofInstant(Instant.ofEpochSecond(loginRequest.getRecordTime()), ZONE_UTC8), loginRequest);
                return loginRequest;

            case CONFIG_SETUP:
                return readTime(in);

            case CONFIG_QUERY:
                return decodeConfigResponse(in);

            case CONTROL:
                return decodeControl(in);

            case CLOCK_CORRECT:
            case HEARTBEAT:
                return null;
            case LOGOUT:
                LogoutRequest logout = LogoutRequest.newBuilder()
                        .setRecordTime(readTime(in))
                        .setLogoutDaySeq(in.readUnsignedShort()).build();
                log.info("登出消息: \n数据采集时间:{}\n{}", ZonedDateTime.ofInstant(Instant.ofEpochSecond(logout.getRecordTime()), ZONE_UTC8),logout);
                return logout;

                default:
                    throw new Error();
        }
    }

    /**
     * 解析控制报文
     * @param in ByteBuf
     * @return 控制报文
     */
    private ControlCommand decodeControl(ByteBuf in) {
        return ControlCommand.newBuilder()
                .setRequestTime(readTime(in))
                .setCommandValue(in.readByte())
                .build();
    }

    /**
     * 解析参数查询应答报文
     * @param in ByteBuf
     * @return 参数查询应答报文
     */
    private ConfigQueryResponse decodeConfigResponse(ByteBuf in) {
        ConfigQueryResponse.Builder builder = ConfigQueryResponse.newBuilder();
        builder.setResponseTime(readTime(in));
        int parameterCount = in.readByte();
        int manageDomainLength = 0, publicDomainLength = 0;
        for (int i = 0; i < parameterCount; i++) {
            Parameter.ParameterCase parameterCase = Parameter.ParameterCase.forNumber(in.readByte());
            Parameter.Builder parameterBuilder = Parameter.newBuilder();
            switch (parameterCase) {
                case STORAGE_PERIOD:
                    parameterBuilder.setStoragePeriod(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case NORMAL_REPORT_INTERVAL:
                    parameterBuilder.setNormalReportInterval(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case ALARM_REPORT_INTERVAL:
                    parameterBuilder.setAlarmReportInterval(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case MANAGEMENT_PLATFORM_DOMAIN_LENGTH:
                    manageDomainLength = in.readByte();
                    parameterBuilder.setManagementPlatformDomainLength(manageDomainLength);
                    builder.addParameters(parameterBuilder.build());
                    break;
                case MANAGEMENT_PLATFORM_DOMAIN:
                    parameterBuilder.setManagementPlatformDomain(in.readCharSequence(manageDomainLength, CHINESE_CHARSET).toString());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case MANAGEMENT_PLATFORM_PORT:
                    parameterBuilder.setManagementPlatformPort(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case PUBLIC_PLATFORM_DOMAIN_LENGTH:
                    publicDomainLength = in.readByte();
                    parameterBuilder.setPublicPlatformDomainLength(publicDomainLength);
                    builder.addParameters(parameterBuilder.build());
                    break;
                case PUBLIC_PLATFORM_DOMAIN:
                    parameterBuilder.setPublicPlatformDomain(in.readCharSequence(publicDomainLength, CHINESE_CHARSET).toString());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case PUBLIC_PLATFORM_PORT:
                    parameterBuilder.setPublicPlatformPort(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case HARDWARE_VERSION:
                    parameterBuilder.setHardwareVersion(in.readCharSequence(5, ASCII_CHARSET).toString());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case FIRMWARE_VERSION:
                    parameterBuilder.setFirmwareVersion(in.readCharSequence(5, ASCII_CHARSET).toString());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case HEARBEAT_INTERVAL:
                    parameterBuilder.setHearbeatInterval(in.readByte());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case TERMINAL_RESPONSE_TIMEOUT:
                    parameterBuilder.setTerminalResponseTimeout(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case PLATFORM_RESPONSE_TIMEOUT:
                    parameterBuilder.setPlatformResponseTimeout(in.readShort());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case LOGIN_RETRY_INTERVAL:
                    parameterBuilder.setLoginRetryInterval(in.readByte());
                    builder.addParameters(parameterBuilder.build());
                    break;
                case SAMPLING:
                    parameterBuilder.setSamplingValue(in.readByte());
                    builder.addParameters(parameterBuilder.build());
                    break;
                    default:
                        log.error("解析未处理的参数类型: {}", parameterCase);

            }
        }
        return builder.build();
    }


    /**
     * 解析 登入数据
     * @param in ByteBuf
     * @return 登入数据
     */
    private LoginRequest decodeLogin(ByteBuf in) {
        LoginRequest.Builder builder = LoginRequest.newBuilder()
                .setRecordTime(readTime(in))
                .setLoginDaySeq(in.readUnsignedShort())
                .setIccid(in.readCharSequence(20, ASCII_CHARSET).toString());
        int count = in.readByte();
        int length = in.readByte();
        builder.setSystemCodeLength(length);
        for (int i = 0; i < count; i++) {
            builder.addChargeableSubsystemCode(
                    in.readCharSequence(length, ASCII_CHARSET).toString());
        }
        return builder.build();
    }


    /**
     * 解析头部数据
     * @param in ByteBuf
     * @return 头部数据
     */
    private FrameHeader decodeFrameHeader(ByteBuf in) {
        frameHeader = FrameHeader.builder()
                .requestType(RequestType.valueOf(in.readByte()))
                .responseTag(ResponseTag.valueOf(in.readByte()))
                .vin(in.readCharSequence(17, GBT32960Message.ASCII_CHARSET).toString())
                .encryptionType(EncryptionType.valueOf(in.readByte()))
                .payloadLength(in.readUnsignedShort())
                .build();
        return frameHeader;
    }

    /**
     * 计算校验码
     * @param in ByteBuf
     * @param start 开始位置
     * @param length 长度
     * @return 校验码
     */
    private byte checkCode(ByteBuf in, int start, int length) {
        if (length == 0) {
            return 0;
        }
        FrameHeader.CheckCodeProcessor processor = new FrameHeader.CheckCodeProcessor();
        in.forEachByte(start, length, processor);
        return processor.getCheckCode();
    }

}
