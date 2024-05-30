package com.longyb.mylive.server.cfg;

import lombok.Data;

/**
 * @author longyubo 2020年1月9日 下午2:29:25
 **/
@Data
public class MyLiveConfig {

	public static MyLiveConfig INSTANCE = null;

	int rtmpPort;
	int httpFlvPort;
	boolean saveFlvFile;
	String saveFlVFilePath;
	int handlerThreadPoolSize;
	boolean enableHttpFlv;
	public int getRtmpPort() {
		return rtmpPort;
	}
	public void setRtmpPort(int rtmpPort) {
		this.rtmpPort = rtmpPort;
	}
	public int getHttpFlvPort() {
		return httpFlvPort;
	}
	public void setHttpFlvPort(int httpFlvPort) {
		this.httpFlvPort = httpFlvPort;
	}
	public boolean isSaveFlvFile() {
		return saveFlvFile;
	}
	public void setSaveFlvFile(boolean saveFlvFile) {
		this.saveFlvFile = saveFlvFile;
	}
	public String getSaveFlVFilePath() {
		return saveFlVFilePath;
	}
	public void setSaveFlVFilePath(String saveFlVFilePath) {
		this.saveFlVFilePath = saveFlVFilePath;
	}
	public int getHandlerThreadPoolSize() {
		return handlerThreadPoolSize;
	}
	public void setHandlerThreadPoolSize(int handlerThreadPoolSize) {
		this.handlerThreadPoolSize = handlerThreadPoolSize;
	}
	public boolean isEnableHttpFlv() {
		return enableHttpFlv;
	}
	public void setEnableHttpFlv(boolean enableHttpFlv) {
		this.enableHttpFlv = enableHttpFlv;
	}
	
	
}
