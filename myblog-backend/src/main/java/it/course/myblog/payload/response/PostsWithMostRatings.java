package it.course.myblog.payload.response;

import it.course.myblog.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class PostsWithMostRatings {
	
	private Post post;
	
	private int countRatings;
}
