/**
 * 
 */
package com.gxws.service.letjoin.webservice;

import java.math.BigDecimal;
import java.util.List;

import javax.jws.WebService;

import com.gxws.service.letjoin.dm.notice.NoticeQueueDM;

/**
 * 通知
 * 
 * @author 朱伟亮
 * @create 2014-5-27 下午5:17:00
 */
@WebService
public interface INoticeBO {
	public void sent(List<NoticeQueueDM> nqdmList);

	public List<NoticeQueueDM> getNewNotice(String without);
	
	public BigDecimal getScanSleep();
}
