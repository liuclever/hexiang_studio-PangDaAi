package com.back_hexiang_studio.pangDaAi.tool.api;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 新闻API工具服务 - 每天60秒读懂世界
 * 为AI助手提供实时新闻信息
 */
@Service
@Slf4j
public class NewsApiToolService {
    
    @Value("${weather.api.base-url:http://localhost:4399}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("获取今日新闻 - 每天60秒读懂世界，包含15条重要新闻")
    public String getTodayNews() {
        log.info("📰 AI Tool: 获取今日新闻 - 每天60秒读懂世界");
        
        try {
            // 调用60s API获取今日新闻
            String url = String.format("%s/v2/60s?encoding=json", baseUrl);
            log.info("🌐 调用60s新闻API: {}", url);
            
            String response = restTemplate.getForObject(url, String.class);
            log.info("📡 60s新闻API响应长度: {}", response != null ? response.length() : "null");
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                
                // 检查60s API响应状态
                if (root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                    JsonNode data = root.get("data");
                    
                    String date = data.get("date").asText();
                    String dayOfWeek = data.get("day_of_week").asText();
                    String lunarDate = data.get("lunar_date").asText();
                    String tip = data.get("tip").asText();
                    JsonNode newsArray = data.get("news");
                    
                    // 构建新闻内容
                    StringBuilder newsContent = new StringBuilder();
                    newsContent.append(String.format("📅 %s %s（农历：%s）\n", date, dayOfWeek, lunarDate));
                    newsContent.append("📰 【每天60秒读懂世界】\n\n");
                    
                    // 添加新闻条目
                    int index = 1;
                    for (JsonNode newsItem : newsArray) {
                        newsContent.append(String.format("%d. %s\n\n", index++, newsItem.asText()));
                    }
                    
                    // 添加每日金句
                    newsContent.append(String.format("💭 每日金句：%s", tip));
                    
                    return newsContent.toString();
                } else {
                    String errorMsg = root.has("message") ? root.get("message").asText() : "API调用失败";
                    log.warn("⚠️ 60s新闻API调用失败，使用模拟数据: {}", errorMsg);
                    return getMockTodayNews();
                }
            } else {
                log.warn("⚠️ 60s新闻API无响应，使用模拟数据");
                return getMockTodayNews();
            }
            
        } catch (RestClientException e) {
            log.error("❌ 新闻API调用失败，使用模拟数据", e);
            return getMockTodayNews();
        } catch (Exception e) {
            log.error("❌ 新闻数据解析失败，使用模拟数据", e);
            return getMockTodayNews();
        }
    }

    @Tool("获取指定日期的新闻，格式：yyyy-MM-dd")
    public String getNewsByDate(@P("要查询的日期，格式为yyyy-MM-dd，如2024-09-18") String date) {
        log.info("📰 AI Tool: 获取指定日期新闻: {}", date);
        
        try {
            // 验证日期格式
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 调用60s API获取指定日期新闻
            String url = String.format("%s/v2/60s?date=%s&encoding=json", baseUrl, date);
            log.info("🌐 调用60s新闻API: {}", url);
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                
                if (root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                    JsonNode data = root.get("data");
                    
                    String queryDate = data.get("date").asText();
                    String dayOfWeek = data.get("day_of_week").asText();
                    String lunarDate = data.get("lunar_date").asText();
                    String tip = data.get("tip").asText();
                    JsonNode newsArray = data.get("news");
                    
                    // 构建新闻内容
                    StringBuilder newsContent = new StringBuilder();
                    newsContent.append(String.format("📅 %s %s（农历：%s）\n", queryDate, dayOfWeek, lunarDate));
                    newsContent.append("📰 【每天60秒读懂世界】\n\n");
                    
                    // 添加新闻条目
                    int index = 1;
                    for (JsonNode newsItem : newsArray) {
                        newsContent.append(String.format("%d. %s\n\n", index++, newsItem.asText()));
                    }
                    
                    // 添加每日金句
                    newsContent.append(String.format("💭 每日金句：%s", tip));
                    
                    return newsContent.toString();
                } else {
                    return String.format("未找到 %s 的新闻数据，可能该日期的数据尚未更新或不存在", date);
                }
            } else {
                return String.format("获取 %s 的新闻失败，请稍后再试", date);
            }
            
        } catch (Exception e) {
            log.error("❌ 获取指定日期新闻失败: {}", e.getMessage(), e);
            return String.format("获取 %s 的新闻失败：%s", date, e.getMessage());
        }
    }

    /**
     * 获取模拟新闻数据
     */
    private String getMockTodayNews() {
        log.info("🎭 使用模拟新闻数据");
        
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dayOfWeek = getDayOfWeekInChinese(today.getDayOfWeek().getValue());
        
        StringBuilder newsContent = new StringBuilder();
        newsContent.append(String.format("📅 %s %s\n", dateStr, dayOfWeek));
        newsContent.append("📰 【每天60秒读懂世界】\n\n");
        
        // 模拟新闻数据
        String[] mockNews = {
            "中国经济持续稳定发展，第三季度GDP同比增长5.2%",
            "科技创新成果显著，多项技术取得重大突破",
            "绿色发展理念深入人心，节能减排成效明显",
            "教育改革持续推进，素质教育全面发展",
            "医疗卫生事业进步显著，民生保障不断完善",
            "数字化转型加速推进，智慧城市建设成果丰硕",
            "乡村振兴战略深入实施，农业现代化水平提升",
            "文化事业繁荣发展，传统文化传承保护加强",
            "对外开放水平不断提高，国际合作持续深化",
            "生态环境治理成效显著，美丽中国建设稳步推进"
        };
        
        for (int i = 0; i < mockNews.length; i++) {
            newsContent.append(String.format("%d. %s\n\n", i + 1, mockNews[i]));
        }
        
        newsContent.append("💭 每日金句：成功不是终点，失败不是末日，最重要的是继续前进的勇气");
        
        return newsContent.toString();
    }

    /**
     * 获取中文星期
     */
    private String getDayOfWeekInChinese(int dayOfWeek) {
        String[] days = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : "未知";
    }
} 