package it.course.myblog.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

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

import it.course.myblog.entity.Comment;
import it.course.myblog.entity.Credit;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.RoleName;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.request.CommentRequest;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.payload.response.CountComments;
import it.course.myblog.repository.CommentRepository;
import it.course.myblog.repository.CreditRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.UserService;

@RestController
@RequestMapping("/comment")
public class CommentController {
	
	@Autowired
	CommentRepository commentRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	CreditRepository creditRepository;
	
	@Autowired
	UserService userService;
	
	
	@PostMapping("/insert-comment")
	@PreAuthorize("hasRole('EDITOR') or hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> createComment(@RequestBody CommentRequest commentRequest, HttpServletRequest request){
			
		Optional<Post> post = postRepository.findById(commentRequest.getId());
		if(!post.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Post not found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		if(post.get().getCredit().getCreditImport() > 0) {
			UserPrincipal up = userService.getAuthenticatedUser(); 
				
			if(up.getAuthorities().stream().filter(g -> g.getAuthority().equals("ROLE_READER")).count() > 0) {
			
				Optional<Users> u = userRepository.findById(up.getId());
				Set<Post> postBoughtList = u.get().getPosts();
				
				if( postBoughtList.stream().filter(p -> (p.getId() == post.get().getId()) ).count() == 0) 
					return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(Instant.now(), 403, null, "You need to buy the relative Post in order to insert a comment", request.getRequestURI()), HttpStatus.FORBIDDEN);
			}
		}
		
		Comment comment = new Comment();	
		comment.setReview(commentRequest.getReview());
		comment.setPost(post.get());
		
		commentRepository.save(comment);
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "New Comment successfully created" , request.getRequestURI()), HttpStatus.OK);
		
	}
	
	@PutMapping("/publish-comment/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> publishComment(@PathVariable Long id, HttpServletRequest request){
		
		Optional<Comment> ccomment = commentRepository.findById(id);
		
		if(!ccomment.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Comment not found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		if(ccomment.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "The Comment was just published", request.getRequestURI()), HttpStatus.FORBIDDEN);
			
		ccomment.get().setVisible(true);
		
		Optional<Credit> credit = creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("C");
		Optional<Users> user = userRepository.findById(ccomment.get().getCreatedBy());
		
		// CREDITS ASSIGNED ONLY IF THE USER ROLE IS 'ROLE_READER'
		if(user.get().getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
			user.get().setCredit(user.get().getCredit() + credit.get().getCreditImport());
			userRepository.save(user.get());
		}
			
		commentRepository.save(ccomment.get());

		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Comment successfully published" , request.getRequestURI()), HttpStatus.OK);
	}
	
	@PutMapping("/unpublish-comment/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> unpublishComment(@PathVariable Long id, HttpServletRequest request){
		
		Optional<Comment> comment = commentRepository.findById(id);
		
		if(!comment.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Comment not found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		if(!comment.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "The Comment was just unpublished", request.getRequestURI()), HttpStatus.FORBIDDEN);
			
		comment.get().setVisible(false);
		
		Optional<Credit> credit = creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("C");
		Optional<Users> user = userRepository.findById(comment.get().getCreatedBy());
		
		// CREDITS SUBTRACTED ONLY IF THE USER ROLE IS 'ROLE_READER'
		if( user.get().getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
			user.get().setCredit(user.get().getCredit() - credit.get().getCreditImport());
			userRepository.save(user.get());
		}
		
		commentRepository.save(comment.get());

		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Comment successfully unpublished" , request.getRequestURI()), HttpStatus.OK);
	}
	
	
	@PutMapping("/update-comment")
	@PreAuthorize("hasRole('EDITOR') or hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> updateComment(@RequestBody CommentRequest commentRequest, HttpServletRequest request){
		
		Optional<Comment> c = commentRepository.findById(commentRequest.getId());
		
		if(!c.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Comment not found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		if(!c.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "The Comment is unpublished", request.getRequestURI()), HttpStatus.FORBIDDEN);
			
		// RECOVER FROM SECURITY CONTEXT THE USER LOGGED IN
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
		
		// THE COMMENT IS UPDATABLE ONLY IF THE USER LOGGED IN IS EQUAL TO COMMENT CREATEDBY
		if(userPrincipal.getId() == c.get().getCreatedBy()) {
			c.get().setReview(commentRequest.getReview());
			c.get().setVisible(false);
			commentRepository.save(c.get());
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Comment successfully updated" , request.getRequestURI()), HttpStatus.OK);
		} else {
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "The Comment is not updatable by you", request.getRequestURI()), HttpStatus.FORBIDDEN);
		}
		
	}
	
	@GetMapping("/count-comments-group-by-post")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> countCommentsGroupByPost(HttpServletRequest request){
		
		// FIND ALL PUBLISHED POSTS
		List<Post> ps = postRepository.findAllByIsVisibleTrue();
		
		List<CountComments> countCommentsGroupByPostList = new ArrayList<CountComments>();
		
		for(Post p : ps) {
			// FIND PUBLISHED COMMENT FOREACH POST
			List<Comment> cs = p.getComments().stream().filter(c -> c.isVisible()).collect(Collectors.toList());
			p.setComments(cs);
			// COUNT PUBLISHED COMMENTS
			long countComments = p.getComments().stream().count();
			countCommentsGroupByPostList.add(new CountComments(p.getTitle(), countComments));
		}
		
		// SORTING LIST BY COUNT DESCENDING
		List<CountComments> countCommentsGroupByPostSortedList = countCommentsGroupByPostList.stream()
			.sorted(Comparator.comparingLong(CountComments::getNrComments)
			.reversed())
			.collect(Collectors.toList());
			
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, countCommentsGroupByPostSortedList, request.getRequestURI()), HttpStatus.OK);
		
	}
	

	@PutMapping("/publish-comments/{ids}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> publishComments(@PathVariable List<Long> ids, HttpServletRequest request){
		
		List<Comment> comments = commentRepository.findByIdIn(ids);
		List<Users> users = new ArrayList<Users>();
		
		Optional<Credit> credit = creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("C");
		
		for(Comment comment : comments) {
			Users user = userRepository.findById(comment.getCreatedBy()).get();
			if( user.getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
				user.setCredit(user.getCredit() + credit.get().getCreditImport());
				users.add(user);
			}
		}
		
		comments.forEach(c -> c.setVisible(true));
		
		commentRepository.saveAll(comments);		
		userRepository.saveAll(users);
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "All selected comments have been published", request.getRequestURI()), HttpStatus.OK);
		
	}
	
	@PutMapping("/unpublish-comments/{ids}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> unpublishComments(@PathVariable List<Long> ids, HttpServletRequest request){
		
		List<Comment> comments = commentRepository.findByIdIn(ids);
		List<Users> users = new ArrayList<Users>();
		
		Optional<Credit> credit = creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("C");
		
		for(Comment comment : comments) {
			Users user = userRepository.findById(comment.getCreatedBy()).get();
			if( user.getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
				user.setCredit(user.getCredit() - credit.get().getCreditImport());
				users.add(user);
			}
		}
		comments.forEach(c -> c.setVisible(false));
		
		commentRepository.saveAll(comments);		
		userRepository.saveAll(users);	
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "All selected comments have been unpublished", request.getRequestURI()), HttpStatus.OK);
		
	}
	
	
	
}
