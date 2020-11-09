package it.course.myblog.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class UserSummary {
	
	private long id;
	private String username;
	private String name;
	private String lastname;
}
