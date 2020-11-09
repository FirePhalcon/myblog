package it.course.myblog.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Blacklist;
import it.course.myblog.entity.BlacklistReason;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Users;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long>{
	
	List<Blacklist> findByUser(Users user);
	
	List<Blacklist> findByUserAndBlacklistedUntilIsNotNull(Users user);
	
	List<Blacklist> findByIsVerifiedFalse();
	
	List<Blacklist> findByCommentIdInAndBlacklistedUntilAfter(List<Long> ids, LocalDate now);
	List<Blacklist> findByPostIdInAndCommentId(List<Long> ids, Long zero);
	
	boolean existsByPostAndReporterAndCommentIdAndBlacklistReason(Users reporter, Post post, Long commentId, BlacklistReason blacklistReason);
	
	List<Blacklist> findByBlacklistedUntilBeforeOrBlacklistedUntilIsNotNull(LocalDate blacklistedUntil);
	
	/* nativeQuery
	@Query(value = "SELECT b.id, b.blacklisted_from, MAX(b.blacklisted_until) AS blacklisted_until, b.user_id, b.post_id, b.comment_id, b.blacklist_reason_id, b.reported_by, b.is_verified  "
			"  FROM Blacklist AS b" + 
			"  WHERE b.blacklistedUntil > ?1" + 
			"  GROUP BY b.user", nativeQuery = true)
	List<Blacklist> bannedUserProfileList(LocalDate localdate);
	 	JPQL Syntax*/
	@Query(value = "  SELECT new Blacklist(b.id,b.blacklistedFrom, max(b.blacklistedUntil) AS blacklistedUntil, b.user, b.post,b.commentId, b.blacklistReason,b.reporter,b.isVerified)" + 
			"  FROM Blacklist AS b" + 
			"  WHERE b.blacklistedUntil > :now" + 
			"  GROUP BY b.user")
	List<Blacklist> bannedUserProfileList(@Param("now") LocalDate localdate);
	
	Optional<Blacklist> findTopByBlacklistedUntilAfterAndUserOrderByBlacklistedUntil(LocalDate localdate, Users user);
}
