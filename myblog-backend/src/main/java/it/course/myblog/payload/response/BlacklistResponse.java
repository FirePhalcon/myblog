package it.course.myblog.payload.response;

import java.time.LocalDate;

import it.course.myblog.entity.Blacklist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class BlacklistResponse {
	
	private long id;
	
	private LocalDate blacklistedFrom;
	
	private LocalDate blacklistedUntil;
	
	private long postId;
	private String postTitle;
	
	private long commentId;
	
	private long reporterId;
	private String reporterUsername;
	
	private long userId;
	private String reportedUsername;
	
	private long blacklistReasonId;
	private String blacklistReasonDescription;
	
	private boolean isVerified;
	
	public static BlacklistResponse create(Blacklist blacklist) {
		
		return new BlacklistResponse(
				
				blacklist.getId(),
				blacklist.getBlacklistedFrom(),
				blacklist.getBlacklistedUntil(),
				blacklist.getPost().getId(),
				blacklist.getPost().getTitle(),
				blacklist.getCommentId(),
				blacklist.getReporter().getId(),
				blacklist.getReporter().getUsername(),
				blacklist.getUser().getId(),
				blacklist.getUser().getUsername(),
				blacklist.getBlacklistReason().getId(),
				blacklist.getBlacklistReason().getReason(),
				blacklist.isVerified()
				);
	}
}
