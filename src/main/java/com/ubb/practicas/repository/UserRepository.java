package com.ubb.practicas.repository;

import com.ubb.practicas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    User findByEmail(String email);
    
    java.util.List<User> findByRoleName(String roleName);
}
