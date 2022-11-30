package test.crawler;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import test.bean.BookIsbn;
import test.config.Memory;
import test.util.CrawlerUtil;
import test.util.ImageBase64Util;
import test.util.LogUtil;
import test.util.StrUtil;

/**
 * 抓取ISBN书号：https://www.kongfz.com/
 */
public class BookIsbnCrawler extends Crawler {
	
	String TAG = "BookIsbnCrawler";
	
	String savePath = Memory.imgSavePath;
	
	Map<String, String> headerMap = new HashMap<String, String>();

	int retryTime = 3;
	
	AtomicInteger atoInt = new AtomicInteger(1);
	
	Set<String> uniqSet = new HashSet<String>();
	
	public void crawl() {
		
		File imgDir = new File(savePath);
		if( !imgDir.exists() ) {
			imgDir.mkdir();
		}
		
		atoInt.addAndGet(uniqSet.size());
		
		headerMap.put("Host", "item.kongfz.com");
		headerMap.put("Referer", "https://www.kongfz.com/");
		headerMap.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
		headerMap.put("accept-encoding", "gzip, deflate, br");
		headerMap.put("accept-language", "zh-CN,zh;q=0.9");
		headerMap.put("cache-control", "max-age=0");
		headerMap.put("Cookie", "PHPSESSID=rh9lknq116ckuo9pgiqb2jouj4; shoppingCartSessionId=a62472297b0c7627de495fa5a03c6587; reciever_area=1006000000; utm_source=101002001000; kfz_uuid=68d0723f-2cf9-4f43-a759-d23153c286e0; kfz_trace=68d0723f-2cf9-4f43-a759-d23153c286e0|0|a5954114894d36e7|101002001000; Hm_lvt_bca7840de7b518b3c5e6c6d73ca2662c=1624890843; Hm_lvt_33be6c04e0febc7531a1315c9594b136=1624890843; kfz-tid=048e6e30d06d6d348cc8f9744e5324b5; TINGYUN_DATA=%7B%22id%22%3A%22XMf0fX2k_0w%23nUhCMQN2SSk%22%2C%22n%22%3A%22WebAction%2FURI%2Findex.php%22%2C%22tid%22%3A%22257dbf3edb0fcdc%22%2C%22q%22%3A0%2C%22a%22%3A298%7D; acw_tc=2760776516249390685651311e6bd932d369439bf419d2356de273bb15146b; Hm_lpvt_33be6c04e0febc7531a1315c9594b136=1624939075; Hm_lpvt_bca7840de7b518b3c5e6c6d73ca2662c=1624939075");
		
		crawlKongFuZi();
		
		LogUtil.logInfo(TAG, "采集任务已完成");
		
	}

	private void crawlKongFuZi() {
		
		Set<String> urlSet = init();
		for(String one : urlSet) {
			String parts[] = one.split("@#@");
			String cat1 = parts[0];
			String cat2 = parts[1];
			String url = parts[2];
			
			String html = null;
			for( int i = 1; i <= retryTime; i ++ ) {
				try {
					if( i == retryTime && Memory.useProxyIp ) {
						html = CrawlerUtil.getHtml(url, false, false, Memory.DEFAULT_TIMEOUT, headerMap);
					} else {
						html = CrawlerUtil.getHtml(url, Memory.useProxyIp, false, Memory.DEFAULT_TIMEOUT, headerMap);
					}
					if( StrUtil.isNotEmpty(html) ) {
						break;
					}
				} catch ( Exception e ) {
					LogUtil.logInfo(TAG, "采集首页报错", e);
				}
			}
			
			if( StrUtil.isNotEmpty(html) ) {
				Document startDoc = Jsoup.parse(html);
				
				try {
					// 获取总页码
					Integer totalPage = Integer.valueOf(startDoc.select("#pageTop span[type=countpage]").text());
					
					parseBook(cat1, cat2, startDoc);
					
					for( int page = 2; page <=totalPage; page++ ) {
						String pageUrl = url.substring(0, url.length() - 1) + "w" + page + "/";
						LogUtil.print("采集页面：" + pageUrl);
						
						for( int i = 1; i <= retryTime; i ++ ) {
							try {
								if( i == retryTime && Memory.useProxyIp ) {
									html = CrawlerUtil.getHtml(pageUrl, false, false, Memory.DEFAULT_TIMEOUT, headerMap);
								} else {
									html = CrawlerUtil.getHtml(pageUrl, Memory.useProxyIp, false, Memory.DEFAULT_TIMEOUT, headerMap);
								}
								if( StrUtil.isNotEmpty(html) ) {
									break;
								}
							} catch ( Exception e ) {
								LogUtil.logInfo(TAG, "采集分页报错", e);
							}
						}
						if( StrUtil.isNotEmpty(html) ) {
							parseBook(cat1, cat2, Jsoup.parse(html));
						}
						
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			}	
		
		}
	
	}

	private void parseBook(final String cat1, final String cat2, Document startDoc) {
		Elements as = startDoc.select("#listBox .img-box");
		for(final Element a : as) {
			
			Memory.threadPool.execute(new Runnable() {
				
				@Override
				public void run() {
					
					try {

						String url = a.attr("href");
						
						LogUtil.print("采集书页：" + url);
						
						// 采集BOOK
						String html = null;
						for( int i = 1; i <= retryTime; i ++ ) {
							try {
								if( i == retryTime && Memory.useProxyIp ) {
									html = CrawlerUtil.getHtml(url, false, false, Memory.DEFAULT_TIMEOUT, headerMap);
								} else {
									html = CrawlerUtil.getHtml(url, Memory.useProxyIp, false, Memory.DEFAULT_TIMEOUT, headerMap);
								}
								if( StrUtil.isNotEmpty(html) ) {
									break;
								}
							} catch ( Exception e ) {
								LogUtil.logInfo(TAG, "采集ISBN报错", e);
							}
						}
						
						if( StrUtil.isNotEmpty(html) ) {
							Document aDoc = Jsoup.parse(html);
							
							BookIsbn bookIsbn = new BookIsbn();
							
							bookIsbn.setCategory1(cat1);
							bookIsbn.setCategory2(cat2);
							
							String bookName = aDoc.select("h1.detail-title").text();
							String bookImg = aDoc.select("#mainInmg").attr("src");
							String author = aDoc.select(".detail-con-right .item.zuozhe .text-value").text();
							
							String authorP = "", translatorP = "";
							String[] parts = author.replaceAll("作者", "").split(",");
							for( String part : parts ) {
								if( StrUtil.isEmpty(part) ) {
									continue;
								}
								if( part.endsWith(" 译") ) {
									translatorP += part.substring(0, part.length() - 2) + ",";
								} else if( part.endsWith(" 著") ) {
									authorP += part.substring(0, part.length() - 2) + ",";
								} else if( part.endsWith(" 编") ) {
									authorP += part.substring(0, part.length() - 2) + ",";
								} else {
									authorP += part + ",";
								}
							}

							if( authorP.endsWith(",") ) {
								authorP = authorP.substring(0, authorP.length()-1);
							}
							if( translatorP.endsWith(",") ) {
								translatorP = translatorP.substring(0, translatorP.length()-1);
							}
							
							bookIsbn.setBookName(bookName);
							bookIsbn.setAuthor(authorP);
							bookIsbn.setTranslator(translatorP);
							
			 				Elements els = aDoc.select(".item");
							for( Element item : els ) {
								String text = item.text();
								
								if( text.startsWith("开本") ) {
									bookIsbn.setSize(item.select(".text-value").text());
								} else if( text.startsWith("出版社") ) {
									bookIsbn.setPublisher(item.select(".text-value").text());
								} else if( text.startsWith("原版书名") ) {
									bookIsbn.setRealName(item.select(".text-value").text());
								} else if( text.startsWith("版次") ) {
									bookIsbn.setPublishTime(item.select(".text-value").text());
								} else if( text.startsWith("纸张") ) {
									bookIsbn.setPaper(item.select(".text-value").text());
								} else if( text.startsWith("出版时间") ) {
									bookIsbn.setPublishDate(item.select(".text-value").text());
								} else if( text.startsWith("页数") ) {
									bookIsbn.setPageNum(item.select(".text-value").text());
								} else if( text.startsWith("字数") ) {
									bookIsbn.setFontNum(item.select(".text-value").text());
								} else if( text.startsWith("ISBN") ) {
									bookIsbn.setIsbn(item.select(".text-value").text());
								} else if( text.startsWith("丛书") ) {
									bookIsbn.setSeries(item.select(".text-value").text());
								} else if( text.startsWith("定价") ) {
									bookIsbn.setPrice(item.select(".text-value").text());
								} else if( text.startsWith("装帧") ) {
									bookIsbn.setSheet(item.select(".text-value").text());
								} else if( text.startsWith("正文语种") ) {
									bookIsbn.setLang(item.select(".text-value").text());
								}
								
							}
							
							if( StrUtil.isEmpty(bookIsbn.getIsbn()) || !uniqSet.add(bookIsbn.getIsbn()) ) {
								return;
							}
							
							if( !"/img/error.jpg".equals(bookImg) ) {
								for( int i = 1; i <= retryTime; i ++ ) {
									try {
										// 下载图片
										String target = atoInt.getAndAdd(1) + ".jpg";
										ImageBase64Util.downloadFile(bookImg, savePath + target, headerMap);
										
										bookIsbn.setImgUrl(target);
										break;
									} catch ( Exception e ) {
										LogUtil.logInfo(TAG, "下载图片失败", e);
									}
								}
								
							}
							
							Elements jianjieels = aDoc.select(".jianjie");
							for( Element item : jianjieels ) {
								String text = item.select("h5").text();
								if( "内容简介: ".equals(text) ) {
									bookIsbn.setSummary(item.text().replaceFirst(text, ""));
								} else if( "作者简介: ".equals(text) ) {
									bookIsbn.setAboutAuthor(item.text().replaceFirst(text, ""));
								} else if( "目录: ".equals(text) ) {
									bookIsbn.setMenu(item.text().replaceFirst(text, ""));
								}
							}
							
							
							crawlToDB(bookIsbn);
							
						}
						// END
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			});

		}
	}

	private Set<String> init() {
		Set<String> urlSet = new HashSet<String>();
		urlSet.add("收藏鉴赏@#@书画篆刻@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k31/");
		urlSet.add("收藏鉴赏@#@玉石@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k32/");
		urlSet.add("收藏鉴赏@#@陶瓷@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k33/");
		urlSet.add("收藏鉴赏@#@家具@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k34/");
		urlSet.add("收藏鉴赏@#@钱币@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k35/");
		urlSet.add("收藏鉴赏@#@邮票@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k36/");
		urlSet.add("收藏鉴赏@#@收藏百科@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k37/");
		urlSet.add("收藏鉴赏@#@杂项@#@http://item.kongfz.com/Cscyjs/tag_k35k39k30k30k38/");
		urlSet.add("小说@#@中国古典小说@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k31/");
		urlSet.add("小说@#@中国现代小说(1919年-1949年)@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k32/");
		urlSet.add("小说@#@中国当代小说(1949年以后)@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k33/");
		urlSet.add("小说@#@四大名著@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k34/");
		urlSet.add("小说@#@世界名著@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k35/");
		urlSet.add("小说@#@外国小说@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k36/");
		urlSet.add("小说@#@侦探/悬疑/推理@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k37/");
		urlSet.add("小说@#@科幻@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k38/");
		urlSet.add("小说@#@情感/家庭/婚姻@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k30k39/");
		urlSet.add("小说@#@穿越/重生@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k30/");
		urlSet.add("小说@#@武侠@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k31/");
		urlSet.add("小说@#@惊悚/恐怖@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k32/");
		urlSet.add("小说@#@魔幻/奇幻/玄幻@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k33/");
		urlSet.add("小说@#@青春/影视@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k34/");
		urlSet.add("小说@#@历史@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k35/");
		urlSet.add("小说@#@官场@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k36/");
		urlSet.add("小说@#@职场@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k37/");
		urlSet.add("小说@#@社会@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k38/");
		urlSet.add("小说@#@军事@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k31k39/");
		urlSet.add("小说@#@财经@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k32k30/");
		urlSet.add("小说@#@作品集@#@http://item.kongfz.com/Cxiaoshuo/tag_k34k33k30k32k31/");
		urlSet.add("文学@#@名家作品@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k31/");
		urlSet.add("文学@#@世界文学@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k36/");
		urlSet.add("文学@#@中国古代文学(1840年以前)@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k32/");
		urlSet.add("文学@#@中国近代文学(1840年-1919年)@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k33/");
		urlSet.add("文学@#@中国现代文学(1919年-1949年)@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k34/");
		urlSet.add("文学@#@中国当代文学(1949年至今)@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k35/");
		urlSet.add("文学@#@诗歌词曲@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k37/");
		urlSet.add("文学@#@散文/随笔/书信@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k38/");
		urlSet.add("文学@#@戏剧与曲艺@#@http://item.kongfz.com/Cwenxue/tag_k31k30k30k39/");
		urlSet.add("文学@#@纪实文学@#@http://item.kongfz.com/Cwenxue/tag_k31k30k31k30/");
		urlSet.add("文学@#@民间文学@#@http://item.kongfz.com/Cwenxue/tag_k31k30k31k31/");
		urlSet.add("文学@#@文学评论与鉴赏@#@http://item.kongfz.com/Cwenxue/tag_k31k30k31k32/");
		urlSet.add("文学@#@文学理论@#@http://item.kongfz.com/Cwenxue/tag_k31k30k31k33/");
		urlSet.add("文学@#@青春文学@#@http://item.kongfz.com/Cwenxue/tag_k31k30k31k34/");
		urlSet.add("语言文字@#@语言学@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k31/");
		urlSet.add("语言文字@#@汉语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k32/");
		urlSet.add("语言文字@#@中国少数民族语言@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k33/");
		urlSet.add("语言文字@#@英语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k34/");
		urlSet.add("语言文字@#@日语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k39/");
		urlSet.add("语言文字@#@韩语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k31k31/");
		urlSet.add("语言文字@#@俄语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k38/");
		urlSet.add("语言文字@#@德语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k36/");
		urlSet.add("语言文字@#@法语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k35/");
		urlSet.add("语言文字@#@西班牙语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k30k37/");
		urlSet.add("语言文字@#@阿拉伯语@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k31k30/");
		urlSet.add("语言文字@#@汉藏语系@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k31k32/");
		urlSet.add("语言文字@#@阿尔泰语系@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k31k33");
		urlSet.add("语言文字@#@印欧语系@#@http://item.kongfz.com/Cyuyan/tag_k31k33k30k31k34/");
		urlSet.add("历史@#@中国史@#@http://item.kongfz.com/Clishi/tag_k33k30k30k31/");
		urlSet.add("历史@#@世界史@#@http://item.kongfz.com/Clishi/tag_k33k30k30k32/");
		urlSet.add("历史@#@地方史志@#@http://item.kongfz.com/Clishi/tag_k33k30k30k33/");
		urlSet.add("历史@#@普及读物@#@http://item.kongfz.com/Clishi/tag_k33k30k30k34/");
		urlSet.add("历史@#@历史研究与评论@#@http://item.kongfz.com/Clishi/tag_k33k30k30k35/");
		urlSet.add("历史@#@史家名著@#@http://item.kongfz.com/Clishi/tag_k33k30k30k36/");
		urlSet.add("历史@#@文物考古@#@http://item.kongfz.com/Clishi/tag_k33k30k30k37/");
		urlSet.add("历史@#@人物传记@#@http://item.kongfz.com/Clishi/tag_k33k30k30k38/");
		urlSet.add("地理@#@中国地理@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k31/");
		urlSet.add("地理@#@世界地理@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k32/");
		urlSet.add("地理@#@风俗习惯@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k33/");
		urlSet.add("地理@#@名胜古迹@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k34/");
		urlSet.add("地理@#@地图@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k35/");
		urlSet.add("地理@#@历史地理@#@http://item.kongfz.com/Cdili/tag_k32k33k30k30k36/");
		urlSet.add("艺术@#@艺术理论@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k31/");
		urlSet.add("艺术@#@绘画@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k32/");
		urlSet.add("艺术@#@书法/篆刻@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k33");
		urlSet.add("艺术@#@雕塑@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k34/");
		urlSet.add("艺术@#@摄影@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k35/");
		urlSet.add("艺术@#@音乐@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k36/");
		urlSet.add("艺术@#@舞蹈@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k37/");
		urlSet.add("艺术@#@设计@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k38/");
		urlSet.add("艺术@#@建筑艺术@#@http://item.kongfz.com/Cyishu/tag_k34k30k30k39/");
		urlSet.add("艺术@#@工艺美术@#@http://item.kongfz.com/Cyishu/tag_k34k30k31k30/");
		urlSet.add("艺术@#@影视/媒体艺术@#@http://item.kongfz.com/Cyishu/tag_k34k30k31k31/");
		urlSet.add("艺术@#@戏剧艺术/舞台艺术@#@http://item.kongfz.com/Cyishu/tag_k34k30k31k32/");
		urlSet.add("艺术@#@动漫/幽默@#@http://item.kongfz.com/Cyishu/tag_k34k30k31k33/");
		urlSet.add("艺术@#@鉴赏收藏@#@http://item.kongfz.com/Cyishu/tag_k34k30k31k34/");
		urlSet.add("政治@#@政治理论@#@http://item.kongfz.com/Czhengzhi/tag_k31k38k30k30k31/");
		urlSet.add("政治@#@中国政治@#@http://item.kongfz.com/Czhengzhi/tag_k31k38k30k30k32/");
		urlSet.add("政治@#@世界政治@#@http://item.kongfz.com/Czhengzhi/tag_k31k38k30k30k33/");
		urlSet.add("政治@#@外交、国际关系@#@http://item.kongfz.com/Czhengzhi/tag_k31k38k30k30k34/");
		urlSet.add("法律@#@法律理论@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k31/");
		urlSet.add("法律@#@国际法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k32/");
		urlSet.add("法律@#@中国法律@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k33/");
		urlSet.add("法律@#@国家法/宪法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k34/");
		urlSet.add("法律@#@行政法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k35/");
		urlSet.add("法律@#@经济法、财政法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k36/");
		urlSet.add("法律@#@民法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k38/");
		urlSet.add("法律@#@刑法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k39/");
		urlSet.add("法律@#@诉讼法/程序法@#@http://item.kongfz.com/Cfalv/tag_k35k30k31k30/");
		urlSet.add("法律@#@司法制度@#@http://item.kongfz.com/Cfalv/tag_k35k30k31k31/");
		urlSet.add("法律@#@商法@#@http://item.kongfz.com/Cfalv/tag_k35k30k30k37/");
		urlSet.add("军事@#@军事理论@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k31/");
		urlSet.add("军事@#@中国军事@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k32/");
		urlSet.add("军事@#@世界军事@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k33/");
		urlSet.add("军事@#@军事史@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k34/");
		urlSet.add("军事@#@古代兵法/战法@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k35/");
		urlSet.add("军事@#@经典军事著作@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k36/");
		urlSet.add("军事@#@武器装备@#@http://item.kongfz.com/Cjunshi/tag_k32k34k30k30k37/");
		urlSet.add("哲学心理学@#@哲学理论@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k31/");
		urlSet.add("哲学心理学@#@世界哲学@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k32/");
		urlSet.add("哲学心理学@#@中国哲学@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k33/");
		urlSet.add("哲学心理学@#@思维科学@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k34/");
		urlSet.add("哲学心理学@#@逻辑学(论理学)@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k35/");
		urlSet.add("哲学心理学@#@伦理学(道德哲学)@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k36/");
		urlSet.add("哲学心理学@#@美学@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k37/");
		urlSet.add("哲学心理学@#@心理学@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k38/");
		urlSet.add("哲学心理学@#@励志与成功@#@http://item.kongfz.com/Czhexue/tag_k34k34k30k30k39/");
		urlSet.add("宗教@#@宗教理论概况分析@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k31/");
		urlSet.add("宗教@#@神话与原始宗教@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k32/");
		urlSet.add("宗教@#@佛教@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k33/");
		urlSet.add("宗教@#@道教@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k34/");
		urlSet.add("宗教@#@伊斯兰教(回教)@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k35/");
		urlSet.add("宗教@#@基督教@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k36/");
		urlSet.add("宗教@#@其他宗教@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k37/");
		urlSet.add("宗教@#@术数、阴阳五行、占卜、命相、堪舆(风水)、巫医巫术@#@http://item.kongfz.com/Czongjiao/tag_k32k39k30k30k38/");
		urlSet.add("经济@#@经济学理论@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k31/");
		urlSet.add("经济@#@中国经济@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k32/");
		urlSet.add("经济@#@世界经济@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k33/");
		urlSet.add("经济@#@行业经济@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k34/");
		urlSet.add("经济@#@会计/审计@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k35/");
		urlSet.add("经济@#@财政税收@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k36/");
		urlSet.add("经济@#@金融@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k37/");
		urlSet.add("经济@#@保险@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k38/");
		urlSet.add("经济@#@贸易@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k30k39/");
		urlSet.add("经济@#@投资理财@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k31k30/");
		urlSet.add("经济@#@市场营销@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k31k31/");
		urlSet.add("经济@#@经济管理@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k31k32/");
		urlSet.add("经济@#@证券/股票@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k31k33/");
		urlSet.add("经济@#@金融银行与货币@#@http://item.kongfz.com/Cjingji/tag_k31k34k30k31k34/");
		urlSet.add("管理@#@管理学@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k31/");
		urlSet.add("管理@#@人力资源管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k32/");
		urlSet.add("管理@#@企业管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k33/");
		urlSet.add("管理@#@市场营销@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k34/");
		urlSet.add("管理@#@财务管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k35/");
		urlSet.add("管理@#@供应链管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k36/");
		urlSet.add("管理@#@项目管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k37/");
		urlSet.add("管理@#@商务实务@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k38/");
		urlSet.add("管理@#@MBA与工商管理@#@http://item.kongfz.com/Cguanli/tag_k32k35k30k30k39/");
		urlSet.add("教育@#@教育学@#@http://item.kongfz.com/Cjiaoyu/tag_k32k38k30k30k31/");
		urlSet.add("教育@#@教育理论@#@http://item.kongfz.com/Cjiaoyu/tag_k32k38k30k30k32/");
		urlSet.add("教育@#@教育心理学@#@http://item.kongfz.com/Cjiaoyu/tag_k32k38k30k30k33/");
		urlSet.add("社会文化@#@社会学@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k31/");
		urlSet.add("社会文化@#@文化@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k32/");
		urlSet.add("社会文化@#@新闻出版@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k33/");
		urlSet.add("社会文化@#@图书馆学@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k34/");
		urlSet.add("社会文化@#@档案学@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k35/");
		urlSet.add("社会文化@#@文化人类学/人口学@#@http://item.kongfz.com/Cshwh/tag_k37k30k30k36/");
		urlSet.add("综合性图书@#@字典辞典@#@http://item.kongfz.com/Czonghe/tag_k32k30k30k30k31/");
		urlSet.add("综合性图书@#@工具书@#@http://item.kongfz.com/Czonghe/tag_k32k30k30k30k32/");
		urlSet.add("综合性图书@#@百科全书/年鉴@#@http://item.kongfz.com/Czonghe/tag_k32k30k30k30k33/");
		urlSet.add("童书@#@幼儿启蒙@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k31/");
		urlSet.add("童书@#@儿童文学@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k32/");
		urlSet.add("童书@#@儿童绘本@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k33/");
		urlSet.add("童书@#@科普百科@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k34/");
		urlSet.add("童书@#@少儿英语@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k35/");
		urlSet.add("童书@#@动漫卡通@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k36/");
		urlSet.add("童书@#@音乐舞蹈@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k37/");
		urlSet.add("童书@#@绘画书法@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k38/");
		urlSet.add("童书@#@儿童手工@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k30k39/");
		urlSet.add("童书@#@智力游戏@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k31k30/");
		urlSet.add("童书@#@婴儿读物@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k31k31/");
		urlSet.add("童书@#@玩具书@#@http://item.kongfz.com/Cshaoer/tag_k32k37k30k31k32/");
		urlSet.add("生活@#@孕产/胎教@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k31/");
		urlSet.add("生活@#@亲子/家教@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k32/");
		urlSet.add("生活@#@旅游/地图@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k33/");
		urlSet.add("生活@#@烹饪/美食@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k34/");
		urlSet.add("生活@#@茶酒饮品@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k35/");
		urlSet.add("生活@#@时尚/美妆@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k36/");
		urlSet.add("生活@#@家庭/家居@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k37/");
		urlSet.add("生活@#@婚恋/两性@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k38/");
		urlSet.add("生活@#@娱乐/休闲@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k30k39/");
		urlSet.add("生活@#@健身/保健@#@http://item.kongfz.com/Cshenghuo/tag_k32k36k30k31k30/");
		urlSet.add("体育@#@体育理论@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k31/");
		urlSet.add("体育@#@奥林匹克@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k32/");
		urlSet.add("体育@#@田径/体操@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k33/");
		urlSet.add("体育@#@球类运动@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k34/");
		urlSet.add("体育@#@武术及民族形式体育@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k35/");
		urlSet.add("体育@#@水上、冰上与雪上运动@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k36/");
		urlSet.add("体育@#@其他体育运动@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k37/");
		urlSet.add("体育@#@棋牌@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k38/");
		urlSet.add("体育@#@文体活动@#@http://item.kongfz.com/Ctiyu/tag_k31k39k30k30k39/");
		urlSet.add("工程技术@#@工业技术@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k31/");
		urlSet.add("工程技术@#@矿业工程@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k32/");
		urlSet.add("工程技术@#@金属学与金属工艺@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k33/");
		urlSet.add("工程技术@#@机械、仪表工业@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k34/");
		urlSet.add("工程技术@#@能源与动力工程@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k35/");
		urlSet.add("工程技术@#@原子能技术@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k36/");
		urlSet.add("工程技术@#@电工技术@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k37/");
		urlSet.add("工程技术@#@电子与通信@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k38/");
		urlSet.add("工程技术@#@化学工业@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k30k39/");
		urlSet.add("工程技术@#@轻工业、手工业@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k31k30/");
		urlSet.add("工程技术@#@建筑@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k31k31/");
		urlSet.add("工程技术@#@水利工程@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k31k32/");
		urlSet.add("工程技术@#@汽车与交通运输@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k31k33/");
		urlSet.add("工程技术@#@航空/航天@#@http://item.kongfz.com/Cjishu/tag_k31k31k30k31k34/");
		urlSet.add("计算机与互联网@#@计算机理论@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k31/");
		urlSet.add("计算机与互联网@#@编程与开发@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k32/");
		urlSet.add("计算机与互联网@#@操作系统@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k33/");
		urlSet.add("计算机与互联网@#@大数据与云计算@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k34/");
		urlSet.add("计算机与互联网@#@图形图像/多媒体@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k35/");
		urlSet.add("计算机与互联网@#@网站设计与网页开发@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k36/");
		urlSet.add("计算机与互联网@#@网络与通讯@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k37/");
		urlSet.add("计算机与互联网@#@硬件、嵌入式开发@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k38/");
		urlSet.add("计算机与互联网@#@办公软件@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k30k39/");
		urlSet.add("计算机与互联网@#@信息安全@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k31k30/");
		urlSet.add("计算机与互联网@#@辅助设计与工程计算@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k31k31/");
		urlSet.add("计算机与互联网@#@软件工程/开发项目管理@#@http://item.kongfz.com/Cjisuanji/tag_k33k31k30k31k32/");
		urlSet.add("自然科学@#@农业/林业@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k31/");
		urlSet.add("自然科学@#@畜牧/养殖@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k32/");
		urlSet.add("自然科学@#@生物科学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k33/");
		urlSet.add("自然科学@#@环境科学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k34/");
		urlSet.add("自然科学@#@数学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k35/");
		urlSet.add("自然科学@#@物理学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k36/");
		urlSet.add("自然科学@#@力学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k37/");
		urlSet.add("自然科学@#@化学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k38/");
		urlSet.add("自然科学@#@天文学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k30k39/");
		urlSet.add("自然科学@#@测绘学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k30/");
		urlSet.add("自然科学@#@地球科学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k31/");
		urlSet.add("自然科学@#@大气科学(气象学)@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k32/");
		urlSet.add("自然科学@#@地质学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k33/");
		urlSet.add("自然科学@#@海洋学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k34/");
		urlSet.add("自然科学@#@自然地理学@#@http://item.kongfz.com/Ckexue/tag_k31k35k30k31k35/");
		urlSet.add("医药卫生@#@医学理论@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k31/");
		urlSet.add("医药卫生@#@预防医学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k32/");
		urlSet.add("医药卫生@#@中医@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k33/");
		urlSet.add("医药卫生@#@基础医学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k34/");
		urlSet.add("医药卫生@#@临床医学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k35/");
		urlSet.add("医药卫生@#@药学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k36/");
		urlSet.add("医药卫生@#@护理学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k37/");
		urlSet.add("医药卫生@#@医院管理@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k38/");
		urlSet.add("医药卫生@#@医疗器械@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k30k39/");
		urlSet.add("医药卫生@#@内科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k30/");
		urlSet.add("医药卫生@#@外科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k31/");
		urlSet.add("医药卫生@#@妇产科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k32/");
		urlSet.add("医药卫生@#@儿科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k33/");
		urlSet.add("医药卫生@#@肿瘤学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k34/");
		urlSet.add("医药卫生@#@神经病学与精神病学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k35/");
		urlSet.add("医药卫生@#@皮肤病学与性病学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k36/");
		urlSet.add("医药卫生@#@耳鼻咽喉科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k37/");
		urlSet.add("医药卫生@#@眼科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k38/");
		urlSet.add("医药卫生@#@口腔科学@#@http://item.kongfz.com/Cyiyao/tag_k31k37k30k31k39/");
		urlSet.add("教材@#@大学教材@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k34/");
		urlSet.add("教材@#@研究生教材@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k35/");
		urlSet.add("教材@#@高职高专教材@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k36/");
		urlSet.add("教材@#@中职中专教材@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k37/");
		urlSet.add("教材@#@成人教育@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k38/");
		urlSet.add("教材@#@职业技术培训@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k39/");
		urlSet.add("教材@#@公共课@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k30/");
		urlSet.add("教材@#@经济管理类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k31/");
		urlSet.add("教材@#@工学类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k32/");
		urlSet.add("教材@#@文法类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k33/");
		urlSet.add("教材@#@医学类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k34/");
		urlSet.add("教材@#@理学类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k35/");
		urlSet.add("教材@#@计算机@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k36/");
		urlSet.add("教辅@#@一年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k31/");
		urlSet.add("教辅@#@二年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k32/");
		urlSet.add("教辅@#@三年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k33/");
		urlSet.add("教辅@#@四年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k34/");
		urlSet.add("教辅@#@五年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k35/");
		urlSet.add("教辅@#@六年级@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k36/");
		urlSet.add("教辅@#@小升初@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k37/");
		urlSet.add("教辅@#@小学通用@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k38/");
		urlSet.add("教辅@#@初一@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k30k39/");
		urlSet.add("教辅@#@初二@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k30/");
		urlSet.add("教辅@#@初三@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k31/");
		urlSet.add("教辅@#@中考@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k32/");
		urlSet.add("教辅@#@初中通用@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k33/");
		urlSet.add("教辅@#@高一@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k34/");
		urlSet.add("教辅@#@高二@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k35/");
		urlSet.add("教辅@#@高三@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k36/");
		urlSet.add("教辅@#@高考@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k37/");
		urlSet.add("教辅@#@高中通用@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k38/");
		urlSet.add("教辅@#@课外阅读@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k31k39/");
		urlSet.add("教辅@#@英语专项@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k30/");
		urlSet.add("教辅@#@语文作文@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k31/");
		urlSet.add("教辅@#@写字/字帖@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k32/");
		urlSet.add("教辅@#@奥数/竞赛@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k32k33/");
		urlSet.add("考试@#@公务员@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k37/");
		urlSet.add("考试@#@考研@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k38/");
		urlSet.add("考试@#@外语考试@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k33k39/");
		urlSet.add("考试@#@司法考试@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k30/");
		urlSet.add("考试@#@会计类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k31/");
		urlSet.add("考试@#@银行类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k32/");
		urlSet.add("考试@#@教师类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k33/");
		urlSet.add("考试@#@医学/药学@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k34/");
		urlSet.add("考试@#@建筑类@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k35/");
		urlSet.add("考试@#@财税外贸保险类考试@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k36/");
		urlSet.add("考试@#@计算机考试@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k37/");
		urlSet.add("考试@#@其他考试@#@http://item.kongfz.com/Cjiaocai/tag_k33k32k30k34k38/");
		return urlSet;
	}

}
