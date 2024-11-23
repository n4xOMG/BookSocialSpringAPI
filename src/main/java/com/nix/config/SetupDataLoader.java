package com.nix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Role;
import com.nix.models.User;
import com.nix.repository.RoleRepository;
import com.nix.repository.UserRepository;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private RoleRepository roleRepo;


	@Autowired
	private PasswordEncoder passEncoder;

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event) {

		createRoleIfNotFound("ADMIN");
		createRoleIfNotFound("USER");

		Role adminRole = roleRepo.findByName("ADMIN");
		User user = userRepo.findByEmail("tanlmht859a@gmail.com");

	    if (user == null) {
	        user = new User();
	        user.setPassword(passEncoder.encode("n/xOMG859"));
	        user.setEmail("tanlmht859a@gmail.com");
	        user.setUsername("Admin");
	        user.setAvatarUrl("https://res.cloudinary.com/ds2ykbawv/image/upload/v1729779030/Chapter_7_reaewqewq/blob_cvrgif.png");
	        user.setBio("I am admin");
	        user.setCredits(9999999);
	        user.setFullname("Duy Tân");
	        user.setGender("Male");
	        user.setRole(adminRole);
	        user.setIsVerified(true);
	        userRepo.save(user);
	    }

	}

	@Transactional
	Role createRoleIfNotFound(String name) {

		Role role = roleRepo.findByName(name);
		if (role == null) {
			role = new Role();
			role.setName(name);
			roleRepo.save(role);
		}
		return role;
	}

}
