package com.vcsm.service;

import com.vcsm.model.EmailLog;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailLogRepository emailLogRepository;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendEventReminder(Event event, User user, String reminderType) {
        String subject = getSubject(reminderType, event.getName());
        String message = buildReminderMessage(event, user, reminderType);
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(message, true);
            
            mailSender.send(mimeMessage);
            
            // Log success
            EmailLog log = new EmailLog(user, event, user.getEmail(), subject, message);
            log.setStatus("SENT");
            emailLogRepository.save(log);
            
            System.out.println("✅ Reminder sent to: " + user.getEmail() + " for event: " + event.getName());
            
        } catch (Exception e) {
            // Log failure
            EmailLog log = new EmailLog(user, event, user.getEmail(), subject, message);
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            emailLogRepository.save(log);
            
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }
    
    private String getSubject(String reminderType, String eventName) {
        switch (reminderType) {
            case "CONFIRMATION":
                return "✅ Registration Confirmed: " + eventName;
            case "DAY_BEFORE":
                return "⏰ Reminder: " + eventName + " is tomorrow!";
            case "HOUR_BEFORE":
                return "🔔 " + eventName + " starts in 1 hour!";
            case "FOLLOW_UP":
                return "📝 How was " + eventName + "?";
            default:
                return "Event Reminder: " + eventName;
        }
    }
    
    private String buildReminderMessage(Event event, User user, String reminderType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = event.getEventDate() != null ? event.getEventDate().format(formatter) : "TBD";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: Arial, sans-serif; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #8b5cf6, #6d28d9); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center; }");
        html.append(".content { padding: 20px; background: #f8f9fa; border-radius: 0 0 10px 10px; }");
        html.append(".event-details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }");
        html.append(".btn { display: inline-block; padding: 10px 20px; background: #8b5cf6; color: white; text-decoration: none; border-radius: 5px; }");
        html.append(".footer { text-align: center; padding: 10px; font-size: 12px; color: #999; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h2>🎉 " + event.getName() + "</h2>");
        html.append("<p>" + getReminderMessage(reminderType) + "</p></div>");
        html.append("<div class='content'>");
        html.append("<div class='event-details'>");
        html.append("<p><strong>📅 Date:</strong> " + formattedDate + "</p>");
        html.append("<p><strong>📍 Location:</strong> " + event.getLocation() + "</p>");
        if (event.getDescription() != null) {
            html.append("<p><strong>📝 Description:</strong> " + event.getDescription() + "</p>");
        }
        html.append("</div>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='http://localhost:8080/events' class='btn'>View Event Details</a>");
        html.append("</p>");
        html.append("<p>Best regards,<br>VCSM Team</p>");
        html.append("</div>");
        html.append("<div class='footer'><small>This is an automated message. Please do not reply.</small></div>");
        html.append("</div></body></html>");
        
        return html.toString();
    }
    
    private String getReminderMessage(String reminderType) {
        switch (reminderType) {
            case "CONFIRMATION": return "You have successfully registered!";
            case "DAY_BEFORE": return "This event is happening tomorrow!";
            case "HOUR_BEFORE": return "This event starts in 1 hour!";
            case "FOLLOW_UP": return "We hope you enjoyed the event!";
            default: return "Event Reminder";
        }
    }
}