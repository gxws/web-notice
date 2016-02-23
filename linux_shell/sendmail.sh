#!/bin/bash

sh_dir=$(cd `dirname $0`; pwd)
base_dir=$sh_dir/..

. $sh_dir/config.sh

mail_context="用户充值记录:$user_out_file_name\n游戏记录:$play_record_file_name"
echo -e $mail_context | mail -s "$mail_title" $mail_box 
