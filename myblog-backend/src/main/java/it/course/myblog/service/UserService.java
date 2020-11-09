package it.course.myblog.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.course.myblog.entity.LoginAttempts;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.LoginAttemptsRepository;
import it.course.myblog.security.UserPrincipal;

@Service
public class UserService {
	
	@Autowired
	LoginAttemptsRepository loginAttemptsRepository;
	
	@Autowired
	PostService postService;
	
	@Value("${app.login.time.to.unlock}")
	private int timeToUnlock;
	
	@Value("${app.login.max.attempts}")
	private int maxAttempts;
	
	public ResponseEntity<?> traceAttempts(Optional<Users> u, HttpServletRequest request) {
		String ip = postService.findIp(request);
		Optional<LoginAttempts> la = Optional.of(new LoginAttempts());
		if(u.isPresent())
			la = loginAttemptsRepository.findTop1ByUserIdOrderByUpdatedAtDesc(u.get().getId());
		else
			la = loginAttemptsRepository.findByIp(ip);
		
		Instant dateLock = Instant.now().minus(timeToUnlock, ChronoUnit.SECONDS);	

		if(la.isPresent()) {
			la.get().setIp(ip);
			
			if(la.get().getAttempts() < maxAttempts) {
				if(la.get().getUpdatedAt().isAfter(dateLock))
					la.get().setAttempts(la.get().getAttempts()+1);
				else
					la.get().setAttempts(1);
				
				loginAttemptsRepository.save(la.get());
			} else { 
				
				la.get().setUpdatedAt(Instant.now());
				
				if(la.get().getUpdatedAt().isAfter(dateLock)) {
					loginAttemptsRepository.save(la.get());
					return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, "Unauthorized", "User locked", request.getRequestURI()), HttpStatus.FORBIDDEN);
				} else {
					la.get().setAttempts(1);
					loginAttemptsRepository.save(la.get());
				}
			}
		} else {
			LoginAttempts l = new LoginAttempts(ip, 1, (u.isPresent() ? u.get().getId() : null));
			loginAttemptsRepository.save(l);
		}	
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, "Unauthorized", "Bad credentials Service", request.getRequestURI()), HttpStatus.FORBIDDEN);
	}
	
	public boolean isLogged() {
		if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() != "anonymousUser")
			return true;
		else
			return false;
	}
	
	public UserPrincipal getAuthenticatedUser() {
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if(authentication == null)
			return null;
		
		return (UserPrincipal) authentication.getPrincipal();
	}
	
	public byte[] getSHA(String input) throws NoSuchAlgorithmException 
    {  
        // Static getInstance method is called with hashing SHA  
        MessageDigest md = MessageDigest.getInstance("SHA-256");  
  
        // digest() method called  
        // to calculate message digest of an input  
        // and return array of byte 
        return md.digest(input.getBytes(StandardCharsets.UTF_8));  
    } 
    
    public String toHexString(byte[] hash) 
    { 
        // Convert byte array into signum representation  
        BigInteger number = new BigInteger(1, hash);  
  
        // Convert message digest into hex value  
        StringBuilder hexString = new StringBuilder(number.toString(16));  
  
        // Pad with leading zeros 
        while (hexString.length() < 32)  
        {  
            hexString.insert(0, '0');  
        }  
  
        return hexString.toString().toUpperCase();  
    } 

}
