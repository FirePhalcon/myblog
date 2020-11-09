package it.course.myblog.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.course.myblog.entity.Post;
import it.course.myblog.entity.Tag;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.TagRepository;

@RestController
@RequestMapping("/tags")
public class TagController {
	
	@Autowired
	TagRepository tagRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@PostMapping("/insert-tag/{tagName}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> insertUser(@PathVariable String tagName, HttpServletRequest request) {
		
		Optional<Tag> tag = tagRepository.findByTagName(tagName);
		
		if(tag.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 401, null, "Tag already inserted with the id: " + tag.get().getId(), request.getRequestURI()), HttpStatus.CONFLICT);
		
		tagRepository.save(new Tag(tagName));
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Tag inserted succesfully", request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/view-all-tags")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> viewAllTags(HttpServletRequest request){
		
		List<Tag> tags = tagRepository.findByOrderByTagNameAsc();
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, tags, request.getRequestURI()), HttpStatus.OK);
	}
	
	@DeleteMapping("/delete-tag/{tagName}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> deleteTag(@PathVariable String tagName, HttpServletRequest request){
		
		Optional<Tag> t = tagRepository.findByTagName(tagName);
		
		if(!t.isPresent()) 
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 404, null, "Tag not found", request.getRequestURI()), HttpStatus.NOT_FOUND);
		
		List<Post> posts = postRepository.findAll();
		
		for(Post p : posts) {
			Set<Tag> ts = p.getTags();
			if(ts.contains(t.get())) {
				ts.remove(t.get());
				p.setTags(ts);
				postRepository.save(p);
			}
		}
		
		tagRepository.delete(t.get());

		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Tag " + tagName + " deleted succesfully", request.getRequestURI()), HttpStatus.OK);
		
		/*if(posts.stream().filter(p -> p.getTags().contains(t.get())).count() == 0){
			return new ResponseEntity<ApiResponse>(new ApiResponse( Instant.now(), 404, null, "Tag already referenced.", request.getRequestURI()), HttpStatus.NOT_FOUND);
		} else {
			tagRepository.delete(t.get());

			return new ResponseEntity<ApiResponse>(new ApiResponse( Instant.now(), 200, null, "Tag deleted succesfully", request.getRequestURI()), HttpStatus.OK);
		}*/
	}
}
