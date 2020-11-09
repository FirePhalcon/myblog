package it.course.myblog.payload.request;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CommentRequest {
	
	@NotNull
	private String review;
	
	@NotNull
	private long id;

}
