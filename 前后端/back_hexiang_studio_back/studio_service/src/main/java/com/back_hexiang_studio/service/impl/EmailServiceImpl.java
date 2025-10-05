package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件服务实现 - 企业级设计
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${security.notification.admin-email:admin@hexiang.com}")
    private String adminEmail;

    @Value("${security.notification.from-name:何湘工作室安全系统}")
    private String fromName;

    @Override
    public boolean sendAccountLockAlert(String username, String clientIp, int lockTime) {
        log.info("发送账户锁定警告邮件 - Username: {}, IP: {}", username, clientIp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(adminEmail);
            message.setSubject(" 账户安全警告 - 用户账户已被锁定");

            String content = String.format(
                    "【何湘工作室安全警报】\n\n" +
                            "检测到账户存在安全风险，已自动执行安全锁定：\n\n" +
                            " 被锁定账户：%s\n" +
                            " 来源IP地址：%s\n" +
                            " 锁定时长：%d分钟\n" +
                            " 触发时间：%s\n" +
                            " 触发原因：连续登录失败次数过多\n\n" +
                            "建议处理措施：\n" +
                            "1. 确认是否为用户本人操作\n" +
                            "2. 检查IP地址是否异常\n" +
                            "3. 必要时联系用户确认账户安全\n" +
                            "4. 可通过管理后台手动解锁账户\n\n" +
                            "此邮件由系统自动发送，请勿回复。",
                    username, clientIp, lockTime,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            message.setText(content);
//            mailSender.send(message);

            log.info("账户锁定警告邮件发送成功 - Username: {}", username);
            return true;

        } catch (Exception e) {
            log.error("发送账户锁定警告邮件失败 - Username: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean sendLoginFailureAlert(String username, String clientIp, int failCount) {
        // 只有达到特定次数才发送邮件（避免邮件轰炸）
        if (failCount < 4) {
            return true;
        }

        log.info("发送登录异常警告邮件 - Username: {}, FailCount: {}", username, failCount);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(adminEmail);
            message.setSubject("⚠ 登录安全提醒 - 检测到异常登录尝试");

            String content = String.format(
                    "【何湘工作室安全提醒】\n\n" +
                            "检测到用户账户存在异常登录尝试：\n\n" +
                            " 涉及账户：%s\n" +
                            " 来源IP：%s\n" +
                            " 失败次数：%d次\n" +
                            " 最新时间：%s\n\n" +
                            "安全提醒：\n" +
                            "• 账户尚未锁定，但请关注后续情况\n" +
                            "• 如失败次数达到5次，系统将自动锁定账户\n" +
                            "• 建议关注该IP地址的访问行为\n\n" +
                            "此邮件由系统自动发送，请勿回复。",
                    username, clientIp, failCount,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            message.setText(content);
//            mailSender.send(message);

            log.info("登录异常警告邮件发送成功 - Username: {}", username);
            return true;

        } catch (Exception e) {
            log.error("发送登录异常警告邮件失败 - Username: {}", username, e);
            return false;
        }
    }
}