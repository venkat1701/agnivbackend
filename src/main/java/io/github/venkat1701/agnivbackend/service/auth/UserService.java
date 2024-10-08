package io.github.venkat1701.agnivbackend.service.auth;

import io.github.venkat1701.agnivbackend.dto.user.UserRegistrationDTO;
import io.github.venkat1701.agnivbackend.exceptions.UserException;
import io.github.venkat1701.agnivbackend.exceptions.UserNotFoundException;
import io.github.venkat1701.agnivbackend.model.Experience;
import io.github.venkat1701.agnivbackend.model.Skill;
import io.github.venkat1701.agnivbackend.model.User;
import io.github.venkat1701.agnivbackend.repository.auth.UserRepository;
import io.github.venkat1701.agnivbackend.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, JwtProvider jwtProvider, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = encoder;
    }

    public User findUserById(Long userId) throws UserNotFoundException {
        Optional<User> user = userRepository.findById(userId);
        if(user.isPresent()) {
            return user.get();
        }

        throw new UserNotFoundException("Username with corresponding id not found");
    }

    public User findUserProfileByJwt(String jwt) throws UserException {
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);
        if(user == null)
            throw new UserException("User with corresponding email not found!");

        return user;
    }

    public User registerUser(UserRegistrationDTO registrationDTO) {
        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setRole(registrationDTO.getRole());
        user.setMobile(registrationDTO.getPhone());
        user.setCreatedAt(LocalDateTime.now());

        user.setExperienceList(registrationDTO.getExperienceList().stream()
                .map(expDTO -> {
                    Experience exp = new Experience();
                    exp.setCompanyName(expDTO.getCompanyName());
                    exp.setJobTitle(expDTO.getJobTitle());
                    exp.setJobDescription(expDTO.getJobDescription());
                    exp.setStartDate(String.valueOf(LocalDate.parse(expDTO.getStartDate())));
                    exp.setEndDate(String.valueOf(LocalDate.parse(expDTO.getEndDate())));
                    exp.setUser(user);
                    return exp;
                })
                .collect(Collectors.toList()));

        user.setSkillList(registrationDTO.getSkillList().stream()
                .map(skillDTO -> {
                    Skill skill = new Skill();
                    skill.setSkill(skillDTO.getSkill());
                    skill.setUser(user);
                    return skill;
                })
                .collect(Collectors.toList()));

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username){
        try {
            User user = userRepository.findByEmail(username);
            if(user==null)
                throw new UserNotFoundException("Username not found against email in records");
            List<GrantedAuthority> authorities = new ArrayList<>();
            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
        } catch(UserNotFoundException unfe) {
            unfe.printStackTrace();
            return null;
        }
    }
}
