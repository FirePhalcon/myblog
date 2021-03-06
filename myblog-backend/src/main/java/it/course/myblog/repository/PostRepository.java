package it.course.myblog.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.DBFile;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Tag;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>{
	
	long countByCreatedBy(Long id);
	
	List<Post> findAllByIsVisibleTrue(Sort sort);
	List<Post> findAllByIsVisibleTrue();
	
	List<Post> findByIdIn(List<Long> ids);
	
	List<Post> findByIsVisibleTrueAndCreatedBy(Long id);
	
	List<Post> findTop2ByIsVisibleTrueOrderByAvgRatingDesc();
	
	List<Post> findByIsVisibleTrueAndContentContaining(String keyword);
	
	Page<Post> findAllByIsVisibleTrue(Pageable paging);

	Page<Post> findByIdIn(List<Long> ids, Pageable paging);
	
	Set<Post> findByTagsInAndIsVisibleTrueOrderByCreatedAtDesc(Set<Tag> tags);
	
	Optional<Post> findByDbFile(DBFile file);
}
