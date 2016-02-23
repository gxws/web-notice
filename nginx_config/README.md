# nginx配置
## 作用原理
1. web-notice项目将会每隔相应时间发送请求到nginx中的web-notice.html地址，web-notice.html将会返回success。<br>
2. 由linux shell在相同的间隔时间检查nginx访问日志。在发现超过相应间隔的时间都没有访问记录，则发送报警邮件到相应的邮箱。<br>

## 使用方式
1. 将web-notice.html放入配置的指定路径。<br>
2. 将monitor.gxwsxx.com.conf放入nginx的vhost路径中。<br>
如果vhost中存在该文件，则将location中的内容添加到配置文件中。<br>