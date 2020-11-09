package it.course.myblog.payload.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.course.myblog.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class PostSearchResponse {
	
	private long postId;
	private String title;
	
	@JsonIgnore
	private String content;
	
	private long createdBy;
	private Date createdAt;
	private long relevance;
	
	public static PostSearchResponse create(Post post) {
		return new PostSearchResponse(
				post.getId(),
				post.getTitle(),
				post.getContent(),
				post.getCreatedBy(),
				post.getCreatedAt(),
				Long.valueOf(0));
	}
}
