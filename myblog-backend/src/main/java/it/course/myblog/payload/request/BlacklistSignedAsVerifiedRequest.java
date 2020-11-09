package it.course.myblog.payload.request;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class BlacklistSignedAsVerifiedRequest {
	
	@NotNull
	private long blacklistId;
	
	@NotNull
	private long blacklistReasonId;
	
	private boolean toBan;

}
