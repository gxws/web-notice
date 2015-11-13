package com.gxws.web.notice.bo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gxws.service.letjoin.dictionary.notice.NoticeStatus;
import com.gxws.service.letjoin.dm.notice.NoticeQueueDM;
import com.gxws.service.letjoin.webservice.INoticeBO;
import com.gxws.tools.object.ObjectTools;
import com.gxws.tools.sign.SignUtil;

/**
 * 消息发送
 * 
 * @author zhuwl120820@gxwsxx.com
 * @since 1.0
 */
@Component
public class NoticeSendBO {

	private Logger log = Logger.getLogger(getClass());

	@Autowired
	private INoticeBO noticeBO;

	private List<NoticeQueueDM> nqdmList = new ArrayList<>();

	private Map<String, Integer> countMap = new HashMap<>();

	private final BigDecimal ms = new BigDecimal("1000");

	// 数字+单位，s秒，m分，h小时，d天，M月，y年
	private String[] intervals = new String[] { "1s", "30s", "3m", "10m", "30m", "1h", "2h", "8h" };

	private Long[] msIntervals = initMsIntervals();

	private long now;

	private int count;

	private List<NoticeQueueDM> returnList;

	private List<NoticeQueueDM> removeList;

	/**
	 * 8次定时发送发送
	 * 
	 * @author 朱伟亮
	 * @create 2014-7-24上午10:57:41
	 * 
	 */
	@Scheduled(cron = "0/1 * * * * ?")
	public void sending8() {
		log.debug("当前队列数：" + nqdmList.size() + "  " + countMap.size());
		now = new Date().getTime();
		returnList = new ArrayList<>();
		removeList = new ArrayList<>();
		for (NoticeQueueDM nqdm : nqdmList) {
			if (null == nqdm || null == nqdm.getId()) {
				continue;
			}
			count = countMap.get(nqdm.getId());
			if (null == countMap.get(nqdm.getId())) {
				countMap.put(nqdm.getId(), 0);
			} else if (msIntervals.length <= count || count < 0) {
				removeNoticeQueueDMFromMap(nqdm);
			}
			long send = nqdm.getInitTime().getTime() + msIntervals[count].longValue();
			if (now <= send) {
				continue;
			} else {
				boolean isSuccess = send(nqdm);
				count = count + 1;
				log.debug("发送处理：" + nqdm.getUrl() + " 发送次数：" + count);
				countMap.put(nqdm.getId(), count);
				if (isSuccess) {
					nqdm.setStatus(NoticeStatus.RECEIVED.getValue());
					removeList.add(nqdm);
					countMap.remove(nqdm.getId());
				} else {
					// if (count >= nqdm.getSendMax().intValue()) {
					if (count >= msIntervals.length) {
						removeNoticeQueueDMFromMap(nqdm);
					}
				}
				nqdm.setTime(new Date());
				returnList.add(nqdm);
			}
		}
		if (0 < returnList.size()) {
			noticeBO.sent(returnList);
		}
		nqdmList.removeAll(removeList);
	}

	/**
	 * 删除队列消息
	 * 
	 * @author zhuwl120820@gxwsxx.com
	 * @param nqdm
	 * @since 1.0
	 */
	private void removeNoticeQueueDMFromMap(NoticeQueueDM nqdm) {
		nqdm.setStatus(NoticeStatus.STOP.getValue());
		removeList.add(nqdm);
		countMap.remove(nqdm.getId());
	}

	/**
	 * 新加列表
	 * 
	 * @author zhuwl120820@gxwsxx.com
	 * @param nqdmList
	 * @since 1.0
	 */
	public void addList(List<NoticeQueueDM> nqdmList) {
		if (null != nqdmList && 0 != nqdmList.size()) {
			for (NoticeQueueDM dm : nqdmList) {
				this.countMap.put(dm.getId(), 0);
				dm.setTime(new Date());
				dm.setInitTime(new Date());
				this.nqdmList.add(dm);
			}
		}
	}

	/**
	 * 获取列表
	 * 
	 * @author 朱伟亮
	 * @create 2014-5-28 下午3:11:22
	 * @return
	 */
	public List<NoticeQueueDM> getList() {
		return nqdmList;
	}

	/**
	 * 发送
	 * 
	 * @author zhuwl120820@gxwsxx.com
	 * @param nqdm
	 * @return true：对方收到并成功处理，false：对方未收到或未处理
	 * @since 1.0
	 */
	private boolean send(NoticeQueueDM nqdm) {
		log.debug("发送：" + nqdm.getUrl());
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost post = new HttpPost(nqdm.getUrl().trim());
			List<NameValuePair> nvpList = new ArrayList<NameValuePair>();
			Map<String, String> dataMap = ObjectTools.string2Map(nqdm.getData(), ",", "=");
			if (null == nqdm.getAppKey() || "".equals(nqdm.getAppKey())) {

			} else {
				dataMap.put("appKey", nqdm.getAppKey());
				String sign = SignUtil.sign(dataMap, null);
				dataMap.put("paySign", sign);
				dataMap.remove("appKey");
			}

			for (Object key : dataMap.keySet()) {
				log.debug("发送参数：" + key.toString() + ":" + dataMap.get(key).toString());
				nvpList.add(new BasicNameValuePair(key.toString(), dataMap.get(key).toString()));
			}
			post.setEntity(new UrlEncodedFormEntity(nvpList, "utf-8"));
			CloseableHttpResponse res = null;
			res = httpclient.execute(post);
			log.info(res.getStatusLine());
			HttpEntity entity = res.getEntity();
			if (null != entity) {
				String str = streamToString(entity.getContent());
				str = str.toLowerCase();
				return str.contains("success");
			}
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				log.debug(e.getMessage(), e);
			}
		}
		return false;
	}

	/**
	 * Stream转换成String
	 * 
	 * @author 朱伟亮
	 * @create 2014-5-28 下午2:18:22
	 * @param is
	 *            InputStream对象
	 * @return String形式返回is中的内容
	 * @throws IOException
	 */
	private String streamToString(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		is.close();
		return sb.toString();
	}

	/**
	 * 把定义的时间转换成毫秒
	 * 
	 * @author zhuwl120820@gxwsxx.com
	 * @return 毫秒
	 * @since 1.0
	 */
	private Long[] initMsIntervals() {
		Long[] msIntervals = new Long[intervals.length];
		BigDecimal b = BigDecimal.ZERO;
		String s = "";
		String v = "";
		BigDecimal bm = new BigDecimal("60");
		BigDecimal bh = new BigDecimal("60");
		BigDecimal bd = new BigDecimal("24");
		BigDecimal bM = new BigDecimal("30");
		BigDecimal by = new BigDecimal("365");
		for (int i = 0; i < intervals.length; i++) {
			s = intervals[i];
			v = s.substring(0, s.length() - 1);
			b = new BigDecimal(v);
			b = b.multiply(ms);
			// 数字+单位，s秒，m分，h小时，d天，M月，y年
			switch (s.substring(s.length() - 1)) {
			case "y":
				b = b.multiply(by);
			case "M":
				b = b.multiply(bM);
			case "d":
				b = b.multiply(bd);
			case "h":
				b = b.multiply(bh);
			case "m":
				b = b.multiply(bm);
			case "s":

			default:
				break;
			}
			msIntervals[i] = new Long(b.longValue());
			log.info(i + ":" + msIntervals[i]);
		}
		return msIntervals;
	}

}
