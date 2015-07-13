/**
 * 
 */
package com.gxws.web.notice.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 
 * @author 朱伟亮
 * @create 2014-5-27 下午6:55:32
 */
@Controller
@RequestMapping("/zhu")
public class TestNoticeSendController {

	private Logger log = Logger.getLogger(getClass());

	@RequestMapping("/testsend")
	public void testSend(HttpServletRequest req, HttpServletResponse res) {
		log.info("-----------------");
		Map<String, String[]> map = req.getParameterMap();
		for (String key : map.keySet()) {
			log.info("key:" + key + "  value:" + Arrays.toString(map.get(key)));

		}
		try {
			PrintWriter pw = res.getWriter();
			double i = Math.random();
			String r = "";
			if (i > 0.5) {
				r = String.valueOf(i) + "success";
			} else {
				r = String.valueOf(i) + "fail";
			}
			pw.write(r);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("+++++++++++++++++");

	}
}
