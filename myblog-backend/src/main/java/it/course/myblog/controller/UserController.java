package it.course.myblog.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.course.myblog.entity.Blacklist;
import it.course.myblog.entity.BlacklistReason;
import it.course.myblog.entity.Comment;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Role;
import it.course.myblog.entity.RoleName;
import it.course.myblog.entity.Tag;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.UserProfile;
import it.course.myblog.payload.request.ChangeRoleRequest;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.payload.response.BannedUserProfile;
import it.course.myblog.payload.response.BlacklistResponse;
import it.course.myblog.payload.response.CreditsByUser;
import it.course.myblog.repository.BlacklistReasonRepository;
import it.course.myblog.repository.BlacklistRepository;
import it.course.myblog.repository.CommentRepository;
import it.course.myblog.repository.CreditRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.RoleRepository;
import it.course.myblog.repository.TagRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	CreditRepository creditRepository;
	
	@Autowired
	CommentRepository commentRepository;
	
	@Autowired
	BlacklistRepository blacklistRepository;
	
	@Autowired
	BlacklistReasonRepository blacklistReasonRepository;
	
	@Autowired
	UserService userService;
	
	@Autowired
	TagRepository tagRepository;
	
	@PostMapping("change-role")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> changeRole(@RequestBody ChangeRoleRequest changeRoleRequest, HttpServletRequest request){
		
		Optional<Users> u = userRepository.findById(changeRoleRequest.getId());
		
		if(!u.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "User Not Found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		Optional<Role> oldRole = roleRepository.findByName(RoleName.valueOf(changeRoleRequest.getOldRoleName()));
		
		if(!oldRole.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "Invalid old user role", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		Optional<Role> newRole = roleRepository.findByName(RoleName.valueOf(changeRoleRequest.getNewRoleName()));
		
		if(!newRole.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "Invalid new user role", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		Set<Role> userRoles = u.get().getRoles();
		
		userRoles.remove(oldRole.get());
		userRoles.add(newRole.get());
		
		u.get().setRoles(userRoles);
		userRepository.save(u.get());
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "User role updated", request.getRequestURI()), HttpStatus.OK);
	}
	
	@PutMapping("/change-newsletter-permission-id/{id}")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> changeNewsLetterPermissionById(@PathVariable Long id, HttpServletRequest request){
		
		Optional<Users> u = userRepository.findById(id);
		
		if(!u.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401,
					"Unauthorized", "Bad credentials", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		u.get().setHasNewsletter(!u.get().isHasNewsletter());
		
		userRepository.save(u.get());
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Newsletter permission updated", request.getRequestURI()), HttpStatus.OK);
	}
	
	@PutMapping("/change-newsletter-permission")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> changeNewsLetterPermission(HttpServletRequest request){
		
		// PEGA O USUÁRIO PELO TOKEN
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
		
		Optional<Users> u = userRepository.findByUsername(userPrincipal.getUsername());
		
		u.get().setHasNewsletter(!u.get().isHasNewsletter());
		
		userRepository.save(u.get());
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Newsletter permission updated", request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/me")
	@PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('MANAGING_EDITOR') or hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> viewMyDetails(HttpServletRequest request){
		
		// PEGA O USUÁRIO PELO TOKEN
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, userPrincipal, request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/view-all-users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> viewAllUsers(HttpServletRequest request){
		
		List<Users> users = userRepository.findAll();
		List<UserProfile> uProfiles = new ArrayList<UserProfile>();
		
		for(Users u : users) {
			uProfiles.add(UserProfile.create(u));
		}
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, uProfiles, request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/view-user-by-username/{username}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> viewUserByUsername(@PathVariable String username, HttpServletRequest request){
		
		Optional<Users> user = userRepository.findByUsername(username);
		
		if(!user.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401,
					"Unauthorized", "Bad credentials", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		UserProfile uProfile = UserProfile.create(user.get());
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, uProfile, request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/view-all-banned-users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> viewAllBannedUsers(HttpServletRequest request){
		
		List<Blacklist> bls = blacklistRepository.bannedUserProfileList(LocalDate.now());
		
		List<BannedUserProfile> bup = new ArrayList<BannedUserProfile>();
		
		for(Blacklist bl : bls) {
			Users u = userRepository.findById(bl.getUser().getId()).get();
			BlacklistReason blr = blacklistReasonRepository.findById(bl.getBlacklistReason().getId()).get();
			List<BlacklistResponse> blacklistUser = blacklistRepository.findByUserAndBlacklistedUntilIsNotNull(u).stream().map(BlacklistResponse::create).collect(Collectors.toList());
			
			bup.add(BannedUserProfile.create(u, bl, blr, blacklistUser));
		}
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, bup, request.getRequestURI()), HttpStatus.OK);
	}
	
	
	@GetMapping("/view-all-credit-by-user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> viewAllCreditsByUser(@PathVariable Long userId, HttpServletRequest request){
		
		Optional<Users> user = userRepository.findById(userId);
		
		if(!user.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401,
					"Unauthorized", "Bad credentials", request.getRequestURI()), HttpStatus.NOT_FOUND);

		List<Post> posts = postRepository.findByIsVisibleTrueAndCreatedBy(userId);
		List<Comment> comments = commentRepository.findByIsVisibleTrueAndCreatedBy(userId);
 		
 		CreditsByUser cbu = new CreditsByUser(userId, user.get().getUsername(), user.get().getCredit(), posts, comments);
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, cbu, request.getRequestURI()), HttpStatus.OK);
	}
	
	@PostMapping("/set-preferred-tags")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> setPreferredTags(@RequestBody Set<Long> ids, HttpServletRequest request){
	
		UserPrincipal userPrincipal = userService.getAuthenticatedUser();
		Users user = userRepository.findById(userPrincipal.getId()).get();
		
		Set<Tag> tags = tagRepository.findByIdIn(ids);
		
		List<String> nameTags = tags.stream().map(t -> t.getTagName()).collect(Collectors.toList());
		
		user.setPreferredTags(tags);
		
		userRepository.save(user);
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom( Instant.now(), 200, null, "Prefered tags for the user " + user.getUsername() + " succesfully updated with " + nameTags.toString(), request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@GetMapping("/get-preferred-tags")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> getPreferredTags(HttpServletRequest request){
		
		UserPrincipal userPrincipal = userService.getAuthenticatedUser();
		Users user = userRepository.findById(userPrincipal.getId()).get();
		
		Set<Tag> tags = user.getPreferredTags();
		
		if(tags.isEmpty())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 404, null, "User " + user.getUsername() + " has not preferred tags", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		else		
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom( Instant.now(), 200, null, tags, request.getRequestURI()),
					HttpStatus.OK);
	}
}
