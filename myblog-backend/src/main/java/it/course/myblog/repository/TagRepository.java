package it.course.myblog.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long>{
	
	List<Tag> findByOrderByTagNameAsc();
	Optional<Tag> findByTagName(String name);
	Set<Tag> findByTagNameIn(Set<String> name);
	Set<Tag> findByIdIn(Set<Long> set);
}
