package com.longyb.mylive.server.handlers;

import static com.longyb.mylive.server.rtmp.Constants.CHUNK_FMT_0;
import static com.longyb.mylive.server.rtmp.Constants.CHUNK_FMT_1;
import static com.longyb.mylive.server.rtmp.Constants.CHUNK_FMT_2;
import static com.longyb.mylive.server.rtmp.Constants.CHUNK_FMT_3;
import static com.longyb.mylive.server.rtmp.Constants.MAX_TIMESTAMP;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.longyb.mylive.server.rtmp.RtmpMessageDecoder;
import com.longyb.mylive.server.rtmp.messages.RtmpMessage;
import com.longyb.mylive.server.rtmp.messages.SetChunkSize;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author longyubo
 * @version 2019年12月14日 下午3:40:18
 *
 */
@Slf4j
public class ChunkDecoder extends ReplayingDecoder<DecodeState> {
	private static Logger log = LoggerFactory.getLogger(ChunkDecoder.class);
	// changed by client command
	int clientChunkSize = 128;

	HashMap<Integer/* csid */, RtmpHeader> prevousHeaders = new HashMap<>(4);
	HashMap<Integer/* csid */, ByteBuf> inCompletePayload = new HashMap<>(4);

	ByteBuf currentPayload = null;
	int currentCsid;

	int ackWindowSize = -1;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		DecodeState state = state();

		if (state == null) {
			state(DecodeState.STATE_HEADER);
		}
		if (state == DecodeState.STATE_HEADER) {
			RtmpHeader rtmpHeader = readHeader(in);
			log.debug("rtmpHeader read:{}", rtmpHeader);

			completeHeader(rtmpHeader);
			currentCsid = rtmpHeader.getCsid();

			// initialize the payload
			if (rtmpHeader.getFmt() != CHUNK_FMT_3) {
				ByteBuf buffer = Unpooled.buffer(rtmpHeader.getMessageLength(), rtmpHeader.getMessageLength());
				inCompletePayload.put(rtmpHeader.getCsid(), buffer);
				prevousHeaders.put(rtmpHeader.getCsid(), rtmpHeader);
			}

			currentPayload = inCompletePayload.get(rtmpHeader.getCsid());
			if (currentPayload == null) {
				// when fmt=3 and previous body completely read, the previous msgLength play the
				// role of length
				RtmpHeader previousHeader = prevousHeaders.get(rtmpHeader.getCsid());
				log.debug("current payload null,previous header:{}", previousHeader);
				currentPayload = Unpooled.buffer(previousHeader.getMessageLength(), previousHeader.getMessageLength());
				inCompletePayload.put(rtmpHeader.getCsid(), currentPayload);
				log.debug("current payload assign as :{}",currentPayload);
			}

			checkpoint(DecodeState.STATE_PAYLOAD);
		} else if (state == DecodeState.STATE_PAYLOAD) {

			final byte[] bytes = new byte[Math.min(currentPayload.writableBytes(), clientChunkSize)];
			in.readBytes(bytes);
			currentPayload.writeBytes(bytes);
			checkpoint(DecodeState.STATE_HEADER);

			if (currentPayload.isWritable()) {
				return;
			}
			inCompletePayload.remove(currentCsid);

			// then we can decode out payload
			ByteBuf payload = currentPayload;
			RtmpHeader header = prevousHeaders.get(currentCsid);

			RtmpMessage msg = RtmpMessageDecoder.decode(header, payload);
			if (msg == null) {
				log.error("RtmpMessageDecoder.decode NULL");
				return;
			}

			if (msg instanceof SetChunkSize) {
				// we need chunksize to decode the chunk
				SetChunkSize scs = (SetChunkSize) msg;
				clientChunkSize = scs.getChunkSize();
				log.debug("------------>client set chunkSize to :{}", clientChunkSize);
			} else {
				out.add(msg);
			}
		}

	}

	private RtmpHeader readHeader(ByteBuf in) {
		RtmpHeader rtmpHeader = new RtmpHeader();

		// alway from the beginning
		int headerLength = 0;

		byte firstByte = in.readByte();
		headerLength += 1;

		// CHUNK HEADER is divided into
		// BASIC HEADER
		// MESSAGE HEADER
		// EXTENDED TIMESTAMP

		// BASIC HEADER
		// fmt and chunk steam id in first byte
		int fmt = (firstByte & 0xff) >> 6;
		int csid = (firstByte & 0x3f);

		if (csid == 0) {//CSID=0表示Basic Header块基本头占用 2 个字节，并且CSID范围在64-319 之间(第二个字节+64(2-63使用1字节表示法，2字节表示法就不需要再表示这些))；
			// 2 byte form
			csid = in.readByte() & 0xff + 64;
			headerLength += 1;
		} else if (csid == 1) {//CSID=1 表示Basic Header块基本头占用3个字节，并且ID范围在64-65599之间(第三个字节*256 + 第二个字节 + 64(2-63使用1字节表示法，3字节表示法就不需要再表示这些))。
			// 3 byte form
			byte secondByte = in.readByte();
			byte thirdByte = in.readByte();
			csid = (thirdByte & 0xff) << 8 + (secondByte & 0xff) + 64;
			headerLength += 2;
		} else if (csid >= 2) {
			//CSID=2 表示该 chunk 是控制信息和一些命令信息，为低版本协议保留的。
			//CSID=3-63 范围内的值表示整个流ID(有效ID)。
			
			// that's it!
		}

		rtmpHeader.setCsid(csid);
		rtmpHeader.setFmt(fmt);

		// basic header complete

		// MESSAGE HEADER
		switch (fmt) {
		case CHUNK_FMT_0: {
			int timestamp = in.readMedium();
			int messageLength = in.readMedium();
			short messageTypeId = (short) (in.readByte() & 0xff);
			int messageStreamId = in.readIntLE();
			headerLength += 11;
			if (timestamp == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

			rtmpHeader.setTimestamp(timestamp);
			rtmpHeader.setMessageTypeId(messageTypeId);
			rtmpHeader.setMessageStreamId(messageStreamId);
			rtmpHeader.setMessageLength(messageLength);

		}
			break;
		case CHUNK_FMT_1: {
			int timestampDelta = in.readMedium();
			int messageLength = in.readMedium();
			short messageType = (short) (in.readByte() & 0xff);

			headerLength += 7;
			if (timestampDelta == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

			rtmpHeader.setTimestampDelta(timestampDelta);
			rtmpHeader.setMessageLength(messageLength);
			rtmpHeader.setMessageTypeId(messageType);
		}
			break;
		case CHUNK_FMT_2: {
			int timestampDelta = in.readMedium();
			headerLength += 3;
			rtmpHeader.setTimestampDelta(timestampDelta);

			if (timestampDelta == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

		}
			break;

		case CHUNK_FMT_3: {
			// nothing
		}
			break;

		default:
			throw new RuntimeException("illegal fmt type:" + fmt);

		}

		rtmpHeader.setHeaderLength(headerLength);

		return rtmpHeader;
	}

	private void completeHeader(RtmpHeader rtmpHeader) {
		RtmpHeader prev = prevousHeaders.get(rtmpHeader.getCsid());
		if (prev == null) {
			return;
		}
		switch (rtmpHeader.getFmt()) {
		case CHUNK_FMT_1:
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
//			rtmpHeader.setTimestamp(prev.getTimestamp());
			break;
		case CHUNK_FMT_2:
//			rtmpHeader.setTimestamp(prev.getTimestamp());
			rtmpHeader.setMessageLength(prev.getMessageLength());
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
			rtmpHeader.setMessageTypeId(prev.getMessageTypeId());
			break;
		case CHUNK_FMT_3:
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
			rtmpHeader.setMessageTypeId(prev.getMessageTypeId());
			rtmpHeader.setTimestamp(prev.getTimestamp());
			rtmpHeader.setTimestampDelta(prev.getTimestampDelta());
			break;
		default:
			break;
		}

	}

}
