package it.course.myblog.payload.request;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class PostViewedDateRequest {
	
	private Instant viewedStart;
	private Instant viewedEnd;
	private long postId;
}
