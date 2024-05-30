package com.longyb.mylive.server.rtmp.messages;

import com.longyb.mylive.server.rtmp.Constants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author longyubo 2019年12月16日 下午3:42:43
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WindowAcknowledgementSize extends RtmpControlMessage {
	int windowSize;

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public WindowAcknowledgementSize(int windowSize) {
		super();
		this.windowSize = windowSize;
	}

	@Override
	public ByteBuf encodePayload() {
		return Unpooled.buffer(4).writeInt(windowSize);
	}
	
	@Override
	public int getMsgType() {
		return Constants.MSG_WINDOW_ACKNOWLEDGEMENT_SIZE;
	}
}
