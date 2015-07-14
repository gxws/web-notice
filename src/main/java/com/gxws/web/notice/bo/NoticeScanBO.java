package com.gxws.web.notice.bo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gxws.service.letjoin.dm.notice.NoticeQueueDM;
import com.gxws.service.letjoin.webservice.INoticeBO;

/**
 * 消息获取
 * 
 * @author zhuwl120820@gxwsxx.com
 * @since 1.0
 */
@Component
public class NoticeScanBO {

	private Logger log = Logger.getLogger(getClass());

	@Autowired
	private NoticeSendBO noticeSendBO;

	@Autowired
	private INoticeBO noticeBO;

	private long last = new Date().getTime();

	private BigDecimal ms = new BigDecimal("1000");

	private long scan = 5000;

	private long now;

	@Scheduled(cron = "0/1 * * * * ?")
	public void scanning() {
		// log.debug("获取新的通知");
		now = new Date().getTime();
		if (last + scan > now) {
			return;
		}
		List<NoticeQueueDM> list = noticeSendBO.getList();
		StringBuffer sb = new StringBuffer();
		for (NoticeQueueDM dm : list) {
			sb.append(dm.getId() + ",");
		}
		String without = "";
		if (0 == sb.length()) {

		} else {
			without = sb.substring(0, sb.length() - 1);
		}
		list = noticeBO.getNewNotice(without);
		if (null != list && 0 != list.size()) {
			log.debug("新通知：" + list.size());
			noticeSendBO.addList(list);
		}
		last = now;
	}

	@Scheduled(cron = "0 0/1 * * * ?")
	public void getScanSleep() {
		scan = noticeBO.getScanSleep().multiply(ms).longValue();
		log.debug("获取scanSleep: " + scan + " ms");
	}
}
