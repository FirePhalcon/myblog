package it.course.myblog.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.course.myblog.entity.Post;
import it.course.myblog.entity.Rating;
import it.course.myblog.entity.RatingUserPostCompositeKey;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.request.RatingRequest;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.RatingRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;

@RestController
@RequestMapping("/rating")
public class RatingController {
	
	@Autowired
	RatingRepository ratingRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@PostMapping("/rate-post")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> ratePost(@RequestBody RatingRequest ratingRequest, HttpServletRequest request){
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
		
		Optional<Users> user = userRepository.findById(userPrincipal.getId());
		if(!user.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "User not found", request.getRequestURI()), HttpStatus.NOT_FOUND);

		Optional<Post> post = postRepository.findById(ratingRequest.getPostId());
		if(!post.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Post not found", request.getRequestURI()), HttpStatus.NOT_FOUND);

		RatingUserPostCompositeKey rupck = new RatingUserPostCompositeKey(post.get(), user.get());
		
		Optional<Rating> rating = ratingRepository.findById(rupck);
		if(rating.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "Rating already inserted with user " + 
						user.get().getUsername() + " and post " + post.get().getId(), request.getRequestURI()), HttpStatus.FORBIDDEN);
		
		if(ratingRequest.getRating() < 1 || ratingRequest.getRating() > 5) {
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "Rating must be between 1 and 5", request.getRequestURI()), HttpStatus.FORBIDDEN);
		}
		
		Rating r = new Rating(ratingRequest.getRating(), rupck);
		
		ratingRepository.save(r);
		
		List<Rating> postRatings = ratingRepository.findByRatingUserPostCompositeKeyPostId(post.get());
		/*int countRatings = postRatings.size() + 1;
		int totalRating = postRatings.stream().mapToInt(i -> rating.get().getRating()).sum() + r.getRating();
		double avgRating = totalRating/countRatings;*/
		
		double avgRating = postRatings.stream()
				.mapToInt(Rating::getRating)
				.average()
				.getAsDouble();
		
		post.get().setAvgRating(avgRating);
		postRepository.save(post.get());
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Rating inserted succesfully", request.getRequestURI()), HttpStatus.OK);
	}
}
