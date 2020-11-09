package it.course.myblog.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Post;
import it.course.myblog.entity.PostViewed;

@Repository
public interface PostViewedRepository extends JpaRepository<PostViewed, Long>{
	
	List<PostViewed> findByViewedStartBetween(Instant begin, Instant end);
	
	List<PostViewed> findByViewedStartBetweenAndPost(Instant begin, Instant end, Post post);
	

}
