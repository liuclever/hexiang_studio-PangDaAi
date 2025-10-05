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
 * 天气查询AI工具服务
 * 为AI助手提供实时天气信息
 */
@Service
@Slf4j
public class WeatherToolService {

    @Value("${weather.api.city:重庆}")
    private String defaultCity;
    
    @Value("${weather.api.base-url:http://apis.juhe.cn}")
    private String baseUrl;
    
    @Value("${weather.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("查询今天的天气情况，包括温度、湿度、天气状况、AQI等信息")
    public String getTodayWeather() {
        log.info("🌤 AI Tool: 查询今天天气，城市: {}", defaultCity);
        
        try {
            // 调用60s API获取实时天气信息
            String url = String.format("%s/v2/weather/forecast?query=%s&days=1", baseUrl, defaultCity);
            log.info(" 调用60s天气API: {}", url);
            
            String response = restTemplate.getForObject(url, String.class);
            log.info(" 60s天气API响应: {}", response);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                
                // 检查60s API响应状态
                if (root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                    JsonNode data = root.get("data");
                    JsonNode location = data.get("location");
                    JsonNode realtime = data.get("realtime");
                    
                    String city = location.has("formatted") ? location.get("formatted").asText() : defaultCity;
                    String temperature = String.valueOf(realtime.get("temperature").asInt());
                    String weather = realtime.get("weather").asText();
                    String humidity = String.valueOf(realtime.get("humidity").asInt());
                    String windDirection = realtime.get("wind_direction").asText();
                    String windStrength = realtime.get("wind_strength").asText();
                    String aqi = String.valueOf(realtime.get("aqi").asInt());
                    
                    // 返回结构化的天气信息，让AI来组织友好的回复
                    return String.format(
                        "城市：%s\n" +
                        "今天日期：%s\n" +
                        "实时温度：%s°C\n" +
                        "天气状况：%s\n" +
                        "湿度：%s%%\n" +
                        "风向：%s\n" +
                        "风力：%s\n" +
                        "空气质量指数：%s",
                        city,
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                        temperature,
                        weather,
                        humidity,
                        windDirection,
                        windStrength,
                        aqi
                    );
                } else {
                    String errorMsg = root.has("message") ? root.get("message").asText() : "API调用失败";
                    log.warn("️ 60s天气API调用失败");
                    return errorMsg;
                }
            } else {
                log.warn(" 60s天气API无响应");
                return "无法获取天气信息";
            }
            
        } catch (RestClientException e) {
            log.warn(" 60s天气API无响应");
            return "无法获取天气信息";
        } catch (Exception e) {
            log.warn(" 60s天气API无响应");
            return "无法获取天气信息";
        }
    }

    @Tool("查询未来指定天数的天气预报，可以根据用户需求查询1-7天")
    public String getWeatherForecast(@P("要查询的天数，如用户说'后3天'则传入3，默认为7天") int days) {
        // 限制天数范围在1-7天之间
        if (days < 1) days = 1;
        if (days > 7) days = 7;
        
        log.info("🌤 AI Tool: 查询{}天天气预报，城市: {}", days, defaultCity);
        
        try {
            // 调用60s API获取指定天数的天气预报
            String url = String.format("%s/v2/weather/forecast?query=%s&days=%d", baseUrl, defaultCity, days);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                
                // 检查60s API响应状态
                if (root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                    JsonNode data = root.get("data");
                    JsonNode location = data.get("location");
                    
                    StringBuilder forecast = new StringBuilder();
                    forecast.append(String.format("【%s】未来%d天天气预报：\n", 
                        location.has("formatted") ? location.get("formatted").asText() : defaultCity, days));
                    
                    // 如果有forecast数组，使用它；否则至少返回今天的信息
                    if (data.has("forecast") && data.get("forecast").isArray()) {
                        JsonNode forecastArray = data.get("forecast");
                        for (JsonNode day : forecastArray) {
                            String date = day.get("date").asText();
                            String weather = day.get("weather").asText();
                            String tempHigh = String.valueOf(day.get("temp_high").asInt());
                            String tempLow = String.valueOf(day.get("temp_low").asInt());
                            String windDirection = day.get("wind_direction").asText();
                            
                            forecast.append(String.format(
                                "%s：%s，%s-%s°C，%s\n",
                                date, weather, tempLow, tempHigh, windDirection
                            ));
                        }
                    } else {
                        // 如果没有预报数据，返回当前天气信息
                        JsonNode realtime = data.get("realtime");
                        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
                        forecast.append(String.format(
                            "%s：%s，%s°C，%s\n",
                            today, 
                            realtime.get("weather").asText(),
                            realtime.get("temperature").asInt(),
                            realtime.get("wind_direction").asText()
                        ));
                    }
                    
                    return forecast.toString().trim();
                } else {
                    String errorMsg = root.has("message") ? root.get("message").asText() : "API调用失败";
                    log.warn(" 60s天气API无响应");
                    return "无法获取天气信息";
                }
            } else {
                log.warn(" 60s天气API无响应");
                return "无法获取天气信息";
            }
            
        } catch (Exception e) {
            log.warn(" 60s天气API无响应");
            return "无法获取天气信息";
        }
    }

} 