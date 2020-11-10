package it.course.myblog.controller;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.course.myblog.entity.Blacklist;
import it.course.myblog.entity.BlacklistReason;
import it.course.myblog.entity.Comment;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Role;
import it.course.myblog.entity.RoleName;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.request.BlacklistRequest;
import it.course.myblog.payload.request.LoginRequest;
import it.course.myblog.payload.request.SignUpRequest;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.payload.response.JwtAuthenticationResponse;
import it.course.myblog.repository.BlacklistReasonRepository;
import it.course.myblog.repository.BlacklistRepository;
import it.course.myblog.repository.CommentRepository;
import it.course.myblog.repository.LoginAttemptsRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.RoleRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.JwtTokenProvider;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.CtrlUserBan;
import it.course.myblog.service.MailService;
import it.course.myblog.service.PostService;
import it.course.myblog.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Api(value = "Users authorization management", description = "All operations about users authorization")
@Slf4j
public class AuthController {
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	JwtTokenProvider tokenProvider;
	
	@Autowired
	BlacklistReasonRepository blacklistReasonRepository;
	
	@Autowired
	CommentRepository commentRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	BlacklistRepository blacklistRepository;
	
	@Autowired
	CtrlUserBan ctrlUserBan;
	
	@Autowired
	MailService mailService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	PostService postService;
	
	@Autowired
	LoginAttemptsRepository loginAttemptsRepository;
	
	@Value("${app.login.max.attempts}")
	private int maxAttempts;
	
	@Value("${app.login.time.to.unlock}")
	private int timeToUnlock;
	
	@PostMapping("/signin")
	@ApiOperation(value = "User login", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "User logged in succesfully"),
			@ApiResponse(code = 401, message = "Bad credentials, user banned or user not verified"),
			@ApiResponse(code = 404, message = "User not found")
	})
	public ResponseEntity<?> authenticatUser(
			@ApiParam(value="LoginRequest Object", required=true) @Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request){
		
		log.info("Call controller authenticatUser with parameter usernameOrEmail {}", loginRequest.getUsernameOrEmail() );
		Optional<Users> u = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(),loginRequest.getUsernameOrEmail());
		
		if(u.isPresent()) {
			if(u.get().getRoles().size() < 1) {
				log.error("User {} not confirmed", loginRequest.getUsernameOrEmail());
				return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, "Unauthorized", "User not confirmed. Please check your email.", request.getRequestURI()), HttpStatus.FORBIDDEN);
			}
			
			if(ctrlUserBan.isBanned(u.get()).isPresent()) {
				log.info("User {} unauthorized to log in. Reason: banned!", loginRequest.getUsernameOrEmail());
				return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, "Unauthorized", "User Banned Until "+ctrlUserBan.isBanned(u.get()).get().getBlacklistedUntil(), request.getRequestURI()), HttpStatus.FORBIDDEN);
			}
		}
		
		Authentication authentication = null;
		try {
			authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
					u.isPresent() ? u.get().getUsername() : loginRequest.getUsernameOrEmail(), loginRequest.getPassword()		
				)
			);
		}catch(BadCredentialsException e) {
			return userService.traceAttempts(u, request);		
		}
			
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		String jwt = tokenProvider.generateToken(authentication);
		log.info("User {} succesfully logged", loginRequest.getUsernameOrEmail());
		
		return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
	}
	
	@PostMapping("/signup")
	@Transactional
	@ApiOperation(value = "User registration", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "User succesfully registered with default role READER"),
			@ApiResponse(code = 403, message = "Username or Email already in use")
	})
	public ResponseEntity<ApiResponseCustom> registerUser(
			@Valid
			@ApiParam(value = "SignUpRequest object", required = true)
			@RequestBody
				SignUpRequest signupRequest,
			HttpServletRequest request){
		
		log.info("Call controller registerUser with SignUpRequest as parameter: {}, {}, {}, {}",
				signupRequest.getEmail(),
				signupRequest.getUsername(),
				signupRequest.getName(),
				signupRequest.getLastname());
		
		if(userRepository.existsByUsername(signupRequest.getUsername())) {
			log.error("Username {} already in use", signupRequest.getUsername());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Username already in use!",	request.getRequestURI()),
					HttpStatus.BAD_REQUEST);
		}
		if(userRepository.existsByEmail(signupRequest.getEmail())) {
			log.error("Email {} already in use", signupRequest.getEmail());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Email already in use!", request.getRequestURI()),
					HttpStatus.BAD_REQUEST);
		}
		
		Users user = new Users(signupRequest.getUsername(), signupRequest.getEmail(),
								signupRequest.getPassword(), signupRequest.getName(), signupRequest.getLastname(),
								signupRequest.isHasNewsletter());
		
		log.info("Encoding password");
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		
		try {
			String identifier = userService.toHexString(userService.getSHA(Instant.now().toString()));
			user.setIdentifierCode(identifier);
			log.info("Verification email for the user {} sent to email {}", user.getUsername(), user.getEmail());
			
			try {
				mailService.send(new String[] {user.getEmail()}, "confirmation", identifier);
			} catch (AddressException e) {
				log.error(e.getMessage());
			} catch (MessagingException e) {
				log.error(e.getMessage());
			}
		} catch (NoSuchAlgorithmException e1) {
			log.error(e1.getMessage());
		}
		
		userRepository.save(user);
		
		log.info("User {} created succesfully with the default role READER", user.getUsername());
		
		try {
			mailService.send(new String[] {user.getEmail()}, "confirmation", user.getEmail());
			log.info("Verification email for the user {} sent to email {}", user.getUsername(), user.getEmail());
		} catch (AddressException e) {
			log.error(e.getMessage());
		} catch (MessagingException e) {
			log.error(e.getMessage());
		}
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "User creation succesfully completed! Please check your email for the verification mail.", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PutMapping("/confirm-signup/{identifierCode}")
	@ApiOperation(value = "Confirm Signup", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Email verified succesfully"),
			@ApiResponse(code = 403, message = "User not found or already confirmed")
	})
	public ResponseEntity<ApiResponseCustom> confirmSignup(
			@ApiParam(value = "Identifier sent via email", required = true)
			@PathVariable
				String identifierCode,
			HttpServletRequest request){
		
		log.info("Call controller confirmSignup identifier {}", identifierCode);
		
		Optional<Users> user = userRepository.findByIdentifierCode(identifierCode);
		
		if(!user.isPresent()) {
			log.error("User with identifier {} not found or already confirmed", identifierCode);
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 403, null, "User not found", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		}
		
		Role userRole = roleRepository.findByName(RoleName.ROLE_READER)
		.orElseThrow( () -> new RuntimeException());

		user.get().setRoles(Collections.singleton(userRole));
		user.get().setIdentifierCode(null);
		
		userRepository.save(user.get());
		
		log.info("User {} succesfully verified", user.get().getUsername());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null,	"User succesfully verified!", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PutMapping("/change-password/{identifierCode}/{newPassword}")//TODO -- Change to have a RequestBody instead of a RequestParam
	@Transactional
	@ApiOperation(value = "Password modification", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Password succesfully modified"),
			@ApiResponse(code = 404, message = "User not found")
	})
	public ResponseEntity<ApiResponseCustom> changePassword(
			@ApiParam(value = "Identifier sent via email", required = true)
			@PathVariable
				String identifierCode,
			@ApiParam(value = "New password of the user", required = true)
			@PathVariable
			@RequestParam
			@Size(min = 5, max = 20)
				String newPassword,
			HttpServletRequest request){
		
		log.info("Call controller changePassword with the parameter identifier: {}", identifierCode);
		
		Optional<Users> user = userRepository.findByIdentifierCode(identifierCode);
		
		if(!user.isPresent()) {
			log.error("User with identifier {} not found", identifierCode);
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 404, null, "User not found", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		}
		
		log.info("Encoding password");
		user.get().setPassword(passwordEncoder.encode(newPassword));
		user.get().setIdentifierCode(null);
		
		userRepository.save(user.get());
		
		log.info("User {} password changed succesfully", user.get().getUsername());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null,	"Password has been modified succesfully!", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PutMapping("/change-password-by-logged-user/{newPassword}")//TODO -- Change to have a RequestBody instead of a RequestParam
	@ApiOperation(value = "Password modification : logged user", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Password has been modified")
	})
	@PreAuthorize("hasRole('READER') or hasRole('EDITOR') or hasRole('MANAGING_EDITOR') or hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> changePasswordByLoggedUser(
			@ApiParam(value = "New password of the user", required = true)
			@PathVariable
			@Size(min = 5, max = 20)
				String newPassword,
			HttpServletRequest request){
		
		UserPrincipal userPrincipal = userService.getAuthenticatedUser();
		
		Users user = userRepository.findById(userPrincipal.getId()).get();
		
		log.info("Call controller changePasswordByLoggedUser with logged user {}", user.getUsername());
		
		log.info("Encoding password");
		user.setPassword(passwordEncoder.encode(newPassword));
		
		userRepository.save(user);
		
		log.info("User {} password changed succesfully", user.getUsername());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null,	"Password has been modified succesfully!", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PutMapping("/forgot-password/{usernameOrEmail}")
	@Transactional
	@ApiOperation(value = "Password modification", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Email has been sent"),
			@ApiResponse(code = 404, message = "User not found")
	})
	public ResponseEntity<ApiResponseCustom> forgotPassword(
			@ApiParam(value = "Username or Email of the user who is going to be modified", required = true)
			@PathVariable
			@Size(min = 3, max = 120)
				String usernameOrEmail,
			HttpServletRequest request){
		
		log.info("Call controller forgotPassword with the parameter usernameOrEmail: {}", usernameOrEmail);
		
		Optional<Users> user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
		
		if(!user.isPresent()) {
			log.error("User {} not found", usernameOrEmail);
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 404, null, "User not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		}
		
		try {
			String identifier = userService.toHexString(userService.getSHA(Instant.now().toString()));
			user.get().setIdentifierCode(identifier);
			userRepository.save(user.get());
			log.info("User {} updated with the identifier {}", user.get().getUsername(), identifier);
			
			try {
				mailService.send(new String[] {user.get().getEmail()}, "forgot", identifier);
			} catch (AddressException e) {
				log.error(e.getMessage());
			} catch (MessagingException e) {
				log.error(e.getMessage());
			}
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage());
		}
		
		log.info("A link with a unique identifier has been sent to email {} from user {}", user.get().getEmail(), user.get().getUsername());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null,	"Email has been sent", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PostMapping("/add-user-to-blacklist")
	@PreAuthorize("hasRole('READER')")
	@ApiOperation(value = "Report user for a inappropried comment or post", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "User succesfully reported"),
			@ApiResponse(code = 400, message = "Post/Comment reported before"),
			@ApiResponse(code = 404, message = "User, Blacklist Reason, Comment or Post not found")
	})
	public ResponseEntity<ApiResponseCustom> addUserToBlacklist(
			@Valid
			@ApiParam(value = "BlacklistRequest object", required = true)
			@RequestBody
				BlacklistRequest blacklistRequest,
			HttpServletRequest request){
		
		log.info("Call controller addUserToBlacklist with BlacklistRequest as parameter: Id reported {}, Id post {}, Id comment {}, Id blacklistreason {}, Id reporter",
						blacklistRequest.getUserId(), blacklistRequest.getPostId(), blacklistRequest.getCommentId(), blacklistRequest.getBlacklistReasonId(),blacklistRequest.getReporter());
		
		Optional<Users> user = userRepository.findById(blacklistRequest.getUserId());
		if(!user.isPresent()) {
			log.error("User id {} not found", blacklistRequest.getUserId());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 404, null, "User not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		}
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
		Optional<Users> reporter = userRepository.findById(userPrincipal.getId());
		
		Optional<BlacklistReason> blr = blacklistReasonRepository.findById(blacklistRequest.getBlacklistReasonId());
		if(!blr.isPresent()) {
			log.error("Blacklistreason id {} not found", blacklistRequest.getBlacklistReasonId());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 404, null, "Blacklist reason not found", request.getRequestURI()), 
					HttpStatus.NOT_FOUND);
		}
		
		
		Post post = new Post();
		Comment comment = new Comment();
		long commentId = 0;
		if(blacklistRequest.getCommentId() > Long.valueOf(0)) {
			
			comment = commentRepository.findById(blacklistRequest.getCommentId()).get();
			if(comment == null) {
				log.error("Comment id {} not found", blacklistRequest.getCommentId());
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom( Instant.now(), 404, null, "Comment not found", request.getRequestURI()),
						HttpStatus.NOT_FOUND);
			}
			
			commentId = comment.getId();
			post = comment.getPost();
		} else {
			post = postRepository.findById(blacklistRequest.getPostId()).get();
			if(post == null) {
				log.error("Post id {} not found", blacklistRequest.getPostId());
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom( Instant.now(), 404, null, "Post not found", request.getRequestURI()),
						HttpStatus.NOT_FOUND);
			}
		}
		
		LocalDate blacklistedFrom = blacklistRequest.getBlacklistedFrom();
		
		boolean isExist = blacklistRepository.existsByPostAndReporterAndCommentIdAndBlacklistReason(reporter.get(), post, commentId, blr.get());
		
		if(isExist) {
			log.info("Post id {} or comment id {} already reported before", blacklistRequest.getPostId(), blacklistRequest.getCommentId());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 400, null, "Post/Comment reported before", request.getRequestURI()),
					HttpStatus.BAD_REQUEST);
		}
		
		
		Blacklist bl = new Blacklist(
				blacklistedFrom,
				null,
				user.get(),
				post,
				commentId,
				blr.get(),
				reporter.get(),
				false
				);

		blacklistRepository.save(bl);
		
		log.info("User {} - {} succesfully added to the blacklist", user.get().getId(), user.get().getUsername());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "User has been added to the blacklist!", request.getRequestURI()),
				HttpStatus.OK);
	}
	
}
