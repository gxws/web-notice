#!/bin/bash

sh_dir=$(cd `dirname $0`; pwd)
base_dir=$sh_dir/..

. $sh_dir/config.sh

echo "检查状态 check_status=1为正常，0为不正常，mail_status=1为已发送，0为未发送"
check_status="1"
mail_status="1"
mail_status_file=$base_dir/mail_status
if [ -e $mail_status_file ] 
then
	mail_status=`cat $mail_status_file`
else
	echo "1" > $mail_status_file
fi

echo "检查字符串"
last_str=`tail -n 30 $nginx_log_file | grep '^192\.168\.200\.28' | sed -n '$p' | awk '{print $4}'`
if [ $last_str ] 
then
	echo "有字符串"
	last_str=${last_str//\[}
	last_str=${last_str//\//\ }
	last_str=${last_str/:/ }
	last_sec=$(date +%s -d "$last_str")
	now_sec=$(date +%s)
	echo $last_sec
	echo $now_sec
	interval_sec=`expr $now_sec - $last_sec`
	if [ $interval_sec -gt $checking_timeout_sec ]
	then
		echo "超时没有收到消息，异常"
		check_status="0"
	fi
else
	echo "没有字符串，异常"
	check_status="0"
fi

if [ $check_status = "0" -a $mail_status = "0" ]
then
	echo "发送邮件"
	echo "$mail_title" | mail -s "$mail_title" $mail_box
	echo "1" > $mail_status_file
fi

if [ $check_status = "1" -a $mail_status = "1" ]
then
	echo "调整回正常状态"
	echo "0" > $mail_status_file
fi

