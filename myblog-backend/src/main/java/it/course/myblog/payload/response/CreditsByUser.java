package it.course.myblog.payload.response;

import java.util.List;

import it.course.myblog.entity.Comment;
import it.course.myblog.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CreditsByUser {
	
	long userId;
	String userName;
	long totalCredits;
	List<Post> posts;
	List<Comment> comments;
}
