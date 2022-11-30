### 用来采集图书的ISBN编号编码、出版社、出版时间、版次、正文语种、定价等信息。

> 本项目介绍了如何使用代理IP和多线程采集公开数据，项目尚不具备使用条件，仅供学习参考

> 项目需要用Maven引入，这里输入引用文本打开后如果有报错，可以检查是否为JDK版本问题

> 运行Starter类启动爬虫

> 如需要使用代理IP，请到[无忧代理IP https://www.data5u.com](http://www.data5u.com)

> 需要修改test.config包下面的Memory类，可以修改

    1. 是否使用代理IP
    2. 图片保存路径
    3. 代理IP的API接口
    4. 线程池数量
    5. 默认超时时间

如果提示 获取代理IP出错： 请到 http://www.data5u.com 获取最新的代理IP-API接口，或者修改Memory.useProxyIp=false 
按照提示关闭代理IP服务即可。
