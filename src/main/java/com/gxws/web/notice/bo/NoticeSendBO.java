package com.gxws.web.notice.bo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	private static final String MONITOR_ID = "monitor_id";
	private static final String MONITOR_URL = "http://monitor.gxwsxx.com:8001/monitor/web-notice.html";
	private static int MONITOR_COUNT = 0;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

	private CloseableHttpClient httpclient = HttpClients.createDefault();

	private Map<String, NoticeQueueDM> nqdmMap = Collections.synchronizedMap(new HashMap<String, NoticeQueueDM>());

	private Map<String, Integer> countMap = Collections.synchronizedMap(new HashMap<String, Integer>());

	private final BigDecimal ms = new BigDecimal("1000");

	// 数字+单位，s秒，m分，h小时，d天，M月，y年
	private String[] intervals = new String[] { "1s", "30s", "3m", "10m", "30m", "1h", "2h", "8h" };

	private Long[] msIntervals = initMsIntervals();

	private long now;

	/**
	 * 8次定时发送发送
	 * 
	 * @author 朱伟亮
	 * 
	 */
	@Scheduled(cron = "0/1 * * * * ?")
	public void sending8() {
		List<NoticeQueueDM> returnList = null;
		Set<String> removeId = null;
		try {
			if (nqdmMap.isEmpty() && countMap.isEmpty()) {

			} else {
				log.info("当前队列数：" + nqdmMap.size() + "  " + countMap.size());
			}
			now = new Date().getTime();
			returnList = new ArrayList<>();
			removeId = new HashSet<>();
			for (NoticeQueueDM nqdm : nqdmMap.values()) {
				if (null == nqdm || null == nqdm.getId()) {
					continue;
				}
				Integer value = countMap.get(nqdm.getId());
				int count = 0;
				if (null == value) {
					countMap.put(nqdm.getId(), new Integer(0));
				} else {
					count = value.intValue();
				}
				if (msIntervals.length <= count || count < 0) {
					nqdm.setStatus(NoticeStatus.STOP.getValue());
					removeId.add(nqdm.getId());
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
						removeId.add(nqdm.getId());
					} else {
						if (count >= msIntervals.length) {
							nqdm.setStatus(NoticeStatus.STOP.getValue());
							removeId.add(nqdm.getId());
						}
					}
					nqdm.setTime(new Date());
					if (!MONITOR_ID.equals(nqdm.getId())) {
						returnList.add(nqdm);
					}
				}
			}
			if (0 < returnList.size()) {
				noticeBO.sent(returnList);
			}
			for (String id : removeId) {
				nqdmMap.remove(id);
				countMap.remove(id);
			}
		} catch (Exception e) {
			nqdmMap = Collections.synchronizedMap(new HashMap<String, NoticeQueueDM>());
			countMap = Collections.synchronizedMap(new HashMap<String, Integer>());
		}
	}

	@Scheduled(cron = "0 0/5 * * * ?")
	public void monitor() {
		log.info("发送监控测试请求");
		NoticeQueueDM mdm = new NoticeQueueDM();
		Date now = new Date();
		MONITOR_COUNT = MONITOR_COUNT + 1;
		mdm.setUrl(MONITOR_URL + "?c=" + MONITOR_COUNT + "&tm=" + now.getTime() + "&ts=" + sdf.format(now));
		mdm.setId(MONITOR_ID);
		addList(Arrays.asList(new NoticeQueueDM[] { mdm }));
	}

	/**
	 * 新加列表
	 * 
	 * @author zhuwl120820@gxwsxx.com
	 * @param nqdmList
	 * @since 1.0
	 */
	public synchronized void addList(List<NoticeQueueDM> nqdmList) {
		if (null != nqdmList && 0 != nqdmList.size()) {
			for (NoticeQueueDM dm : nqdmList) {
				this.countMap.put(dm.getId(), 0);
				dm.setTime(new Date());
				dm.setInitTime(new Date());
				// this.nqdmList.add(dm);
				this.nqdmMap.put(dm.getId(), dm);
			}
		}
	}

	/**
	 * 获取列表
	 * 
	 * @author 朱伟亮
	 * @return
	 */
	public String ids() {
		StringBuffer sb = new StringBuffer();
		for (String key : nqdmMap.keySet()) {
			sb.append(key + ",");
		}
		String without = "";
		if (0 == sb.length()) {

		} else {
			without = sb.substring(0, sb.length() - 1);
		}
		return without;
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
		CloseableHttpResponse res = null;
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
			if (res != null) {
				try {
					res.close();
				} catch (IOException e) {
					log.debug(e.getMessage(), e);
				}
			}
		}
		return false;
	}

	/**
	 * Stream转换成String
	 * 
	 * @author 朱伟亮
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
