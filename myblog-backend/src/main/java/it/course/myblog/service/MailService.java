package it.course.myblog.service;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService {
	
	@Value("${smtp.host.name}")
	private String hostName;
	
	@Value("${smtp.host.port}")
	private String hostPort;	
	
	@Value("${smtp.email.sender}")
	private String emailSender;
	
	@Value("${smtp.password.sender}")
	private String passwordSender;
	
    public void send(String[] email, String reason, String aux) throws AddressException, MessagingException  {
        Properties props = System.getProperties();
        props.put("mail.smtp.socketFactory.port", hostPort);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.host", hostName);
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.ssl.enable", "true");
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailSender));
        InternetAddress[] addTo = new InternetAddress[email.length];
        for (int i = 0; i < addTo.length; i++) {
            addTo[i] = new InternetAddress(email[i]);
        }
 
        message.setRecipients(Message.RecipientType.TO, addTo);
        
        switch(reason) {
        case "forgot":
        	message.setSubject("MyBlog: forgot password");
            message.setText("In order to change your password click on the link below:\n "
            		+ "http://localhost:8081/api/auth/change-password/"+aux);
        break;
        case "confirmation":
        	message.setSubject("MyBlog: confirm your registration");
            message.setText("In order to verify your email adress click on the link below:\n "
            		+ "http://localhost:8081/api/auth/confirm-signup/"+aux);
        break;}
	        
        Transport transport = session.getTransport("smtp");
        transport.connect(hostName, emailSender, passwordSender);
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }

}
