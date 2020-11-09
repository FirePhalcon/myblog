package it.course.myblog.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.course.myblog.entity.Post;
import it.course.myblog.entity.Tag;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.UserService;

@RestController
@RequestMapping("/preferred-tags")
public class PreferredTagsController {
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	UserService userService;
	
	@Autowired
	UserRepository userRepository;
	
	@GetMapping("/gets-posts-by-preferred-tags")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> getPostsByPreferredTags(HttpServletRequest request) {

		UserPrincipal userPrincipal = userService.getAuthenticatedUser();
		Users user = userRepository.findById(userPrincipal.getId()).get();
		
		Set<Tag> tags = user.getPreferredTags();
		
		if(tags.isEmpty())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "User " + user.getUsername() + " has no preferred tags", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		
		Set<Post> posts = postRepository.findByTagsInAndIsVisibleTrueOrderByCreatedAtDesc(tags);
		
		List<String> nameTags = tags.stream().map(t -> t.getTagName()).collect(Collectors.toList());
		
		if(posts.isEmpty())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "No post found with the tags " + nameTags.toString(), request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, posts, request.getRequestURI()),
				HttpStatus.OK);
	}
}
