package it.course.myblog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>{
	
	List<Comment> findAllByIsVisibleTrue();
	List<Comment> findByIdIn(List<Long> ids);
	
	List<Comment> findByIsVisibleTrueAndCreatedBy(long createdBy);

}
