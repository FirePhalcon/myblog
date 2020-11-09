package it.course.myblog.payload.response;

import java.util.Set;

import it.course.myblog.entity.Post;
import it.course.myblog.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class PostTagResponse {

	private long postId;
	private String title;
	private Set<Tag> tags;
	private Double relevance;
	
	public static PostTagResponse create(Post post) {
		return new PostTagResponse(
				post.getId(),
				post.getTitle(),
				post.getTags(),
				Double.valueOf(0));
	}
}
