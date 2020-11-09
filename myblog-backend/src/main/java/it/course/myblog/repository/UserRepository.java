package it.course.myblog.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Role;
import it.course.myblog.entity.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>{
	
	Optional<Users> findByUsername(String username);
	List<Users> findByUsernameLike(String username);
	
	Optional<Users> findByIdAndRolesIn(Long id, Set<Role> role);
	
	Boolean existsByUsername(String username);
	Boolean existsByEmail(String email);
	
	Optional<Users> findByEmail(String email);
	Optional<Users> findByUsernameOrEmail(String username, String email);
	List<Users> findByLastnameLike(String lastname);
	List<Users> findUserByHasNewsletterTrue();
	
	List<Users> findByRolesOrIdIn(Role role, List<Long> ids);
	
	Optional<Users> findByIdentifierCode(String identifierCode);
	/*Optional<Users> deleteByUsername(String username);*/
}
