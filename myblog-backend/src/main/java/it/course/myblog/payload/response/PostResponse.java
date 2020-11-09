package it.course.myblog.payload.response;

import java.util.Date;
import java.util.List;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import it.course.myblog.entity.Comment;
import it.course.myblog.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PostResponse {
	
	private long id;
	private String title;
	private String content;
	private long updatedBy;
	
	@Temporal(TemporalType.DATE)
	private Date updatedAt;
	
	List<Comment> comments;
	
	private long visited;
	
	public static PostResponse create(Post post) {
		return new PostResponse(
				post.getId(),
				post.getTitle(),
				post.getContent(),
				post.getUpdatedBy(),
				post.getUpdatedAt(),
				post.getComments(),
				Long.valueOf(0));
	}
}
