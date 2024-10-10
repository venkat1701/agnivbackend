package io.github.venkat1701.agnivbackend.repository.auth;

import io.github.venkat1701.agnivbackend.exceptions.UserNotFoundException;
import io.github.venkat1701.agnivbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Transactional
    User findByEmail(String email) throws UserNotFoundException;
}
