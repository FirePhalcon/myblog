package it.course.myblog.payload.request;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangeRoleRequest {
	
	@NotNull
	private long id;
	
	@NotNull
	private String oldRoleName;
	
	@NotNull
	private String newRoleName;

}
