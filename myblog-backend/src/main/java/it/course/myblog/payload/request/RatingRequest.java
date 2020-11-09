package it.course.myblog.payload.request;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor @NoArgsConstructor
public class RatingRequest {
	@NotNull
	private long postId;
	
	@NotNull
	private int rating;

}
