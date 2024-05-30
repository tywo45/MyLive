package com.longyb.mylive.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.longyb.mylive.server.handlers.ChunkDecoder;
import com.longyb.mylive.server.handlers.ChunkEncoder;
import com.longyb.mylive.server.handlers.ConnectionAdapter;
import com.longyb.mylive.server.handlers.HandShakeDecoder;
import com.longyb.mylive.server.handlers.RtmpMessageHandler;
import com.longyb.mylive.server.manager.StreamManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RTMPServer {
	private static Logger log = LoggerFactory.getLogger(RTMPServer.class);

	private int port;

	ChannelFuture channelFuture;

	EventLoopGroup	eventLoopGroup;
	StreamManager	streamManager;
	int				handlerThreadPoolSize;

	public RTMPServer(int port, StreamManager sm, int threadPoolSize) {
		this.port = port;
		this.streamManager = sm;
		this.handlerThreadPoolSize = threadPoolSize;
	}

	public void run() throws Exception {
		eventLoopGroup = new NioEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		DefaultEventExecutorGroup executor = new DefaultEventExecutorGroup(handlerThreadPoolSize);
		b.group(eventLoopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline()//
				        .addLast(new ConnectionAdapter())//
				        .addLast(new HandShakeDecoder())//
				        .addLast(new ChunkDecoder())//
				        .addLast(new ChunkEncoder())//
				        .addLast(executor, new RtmpMessageHandler(streamManager));//
			}
		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

		channelFuture = b.bind(port).sync();

		log.info("RTMP Server start , listen at :{}", port);

	}

	public void close() {
		try {
			channelFuture.channel().closeFuture().sync();
			eventLoopGroup.shutdownGracefully();
		} catch (Exception e) {
			log.error("close rtmp server failed", e);
		}
	}

}
