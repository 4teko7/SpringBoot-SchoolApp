package com.teko.commercial.controllers;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.teko.commercial.Entities.Role;
import com.teko.commercial.Entities.User;
import com.teko.commercial.Entities.Video;
import com.teko.commercial.encryption.EncodeDecode;
import com.teko.commercial.repositories.RoleRepository;
import com.teko.commercial.services.UserDetailsServiceImp;
import com.teko.commercial.services.VideoService;
import com.teko.commercial.util.FileUtils;
import com.teko.commercial.utils.CheckRoles;
import com.teko.commercial.validator.UserValidator;

@RequestMapping("/secured")
@Controller
public class SecuredController {
	final String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static";
	
	@Autowired
	private UserDetailsServiceImp userService;
	
	@Autowired
	private VideoService videoService;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private UserValidator validator;
	
	private EncodeDecode encodeDecode = new EncodeDecode();
	
	private FileUtils fileUtils = new FileUtils();
	
	CheckRoles checkRoles = new CheckRoles();

	@GetMapping("/users")
	public String getAllUsers(Model theModel,Principal principle,Authentication authentication,HttpSession session, HttpServletRequest request,  ModelMap   modelMap) {
		List<User> users = userService.findAll();
		theModel.addAttribute("users",users);
		if(!checkRoles.hasRole("ROLE ADMIN")) {
			theModel.addAttribute("message","Sadece Adminler Bu Sayfayi Gorebilir !!!");
			return "home";
		} 
		
		return "users";

	}
	
//	@PreAuthorize("hasAnyRole('ADMIN')")
//	@RequestMapping(value="adduser", method = RequestMethod.GET)
//	public String showFormForAddUser(Model theModel) {
//		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
//		User user = new User();
//		theModel.addAttribute("user",user);
//		return "addUserForm";
//	}
	
//	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(value="adduser", method = RequestMethod.POST)
	public String addUser(HttpServletRequest request, @ModelAttribute("user") User user) {
		if(request.getParameter("makeAdmin") != null) {
			Role role = new Role();
			role.setid(1);
			role.setRole("ADMIN");
			user.setRoles(Arrays.asList(role));
		}else if(request.getParameter("makeUser") != null) {
			Role role = new Role();
			role.setid(2);
			role.setRole("USER");
			user.setRoles(Arrays.asList(role));
		}
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		userService.save(user);
		return "redirect:/secured/users";
	}
	
//	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(value="deleteuser", method = RequestMethod.GET)
	public String deleteUser(@RequestParam("id") int theId) {
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		userService.deleteUser(theId);
		return "redirect:/secured/users";
	}
	
//	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(value="updateuser", method = RequestMethod.GET)
	public String updateUserGet(@RequestParam("id") int theId,Model theModel) {
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		User user = userService.findById(theId);
		user.setPassword(encodeDecode.decode(user.getPassword()));
		user.setPasswordConfirm(encodeDecode.decode(user.getPasswordConfirm()));
		theModel.addAttribute("user",user);
		return "updateUser";
	}
	
	@RequestMapping(value="updateuser", method = RequestMethod.POST)
	public String updateUserPost(HttpServletRequest request, @ModelAttribute("user") User user,Model theModel,Authentication authentication) {
		
		
		if(authentication != null && authentication.isAuthenticated()) {
			
			
			User thisUser = userService.findById(user.getId());
			String errors = validator.validateForProfileUpdate(user);
			
			if (!errors.equals("")) {
				userService.updateUser(thisUser, user);
				theModel.addAttribute("user",thisUser);
				theModel.addAttribute("error",errors);
	            return "updateUser";
	        }else {
	        	if(request.getParameter("makeAdmin") != null) {
	    			Role role = new Role();
	    			role.setid(1);
	    			role.setRole("ADMIN");
	    			List<Role> lst = new ArrayList<Role>();
	    			lst.add(role);
	    			user.setRoles(lst);
	    		}else if(request.getParameter("makeUser") != null) {
	    			Role role = new Role();
	    			role.setid(2);
	    			role.setRole("USER");
	    			List<Role> lst = new ArrayList<Role>();
	    			lst.add(role);
	    			user.setRoles(lst);
	    		}
				userService.updateUser(thisUser,user);
				userService.save(thisUser);
	        }
			
		}
		
		
		
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";

		return "redirect:/secured/users";
	}
	
	
	@RequestMapping(value="myvideos", method = RequestMethod.GET)
	public String myvideos(Model theModel,HttpServletRequest request) {
		
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		User thisUser = userService.findByUsername(request.getRemoteUser());
		List<Video> videos = videoService.findByUser(thisUser);
		System.out.println("VIDEOS : " + videos);
		theModel.addAttribute("videos",videos);
		theModel.addAttribute("video",new Video());
		
		return "myvideos";
	}
	
	@PostMapping("/uploadvideo")
	public String uploadVideo(@RequestParam("file") MultipartFile file,@ModelAttribute("video") Video videoFromPage,Model theModel,Authentication authentication,HttpServletRequest request) {
		if(authentication != null && authentication.isAuthenticated()) {
//			System.out.println(userService.findByUsername(request.getRemoteUser()));
			
			User thisUser = userService.findByUsername(request.getRemoteUser());
			Video video = userService.uploadVideo(thisUser, file);
			System.out.println("Video Returned");
			System.out.println(videoFromPage);
			video.setVideoContent(videoFromPage.getVideoContent());
			video.setVideoSubject(videoFromPage.getVideoSubject());
			video.setName(videoFromPage.getName());
			videoService.save(video);
			System.out.println("AFTER SAVING");
			userService.save(thisUser);
			
			theModel.addAttribute("message","Successfully Uploaded.");
			List<Video> videos = videoService.findByUser(thisUser);
			System.out.println("VIDEOS : " + videos);
			theModel.addAttribute("videos",videos);
			return "myvideos";
        }
//		
    	return "redirect:/secured/myvideos";

	}
	
	
	@RequestMapping(value="addvideopage", method = RequestMethod.GET)
	public String addvideoPage(Model theModel,HttpServletRequest request) {
		
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		
		theModel.addAttribute("video",new Video());
		
		return "uploadvideo";
	}
	
	@RequestMapping(value="updatevideo", method = RequestMethod.GET)
	public String updateVideoGet(@RequestParam("id") int theId, Model theModel) {
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		
		Video video = videoService.findById(theId);
		theModel.addAttribute("video",video);
		return "updatevideo";
	}
	
	@RequestMapping(value="updatevideo", method = RequestMethod.POST)
	public String updateVideoPost(HttpServletRequest request, @ModelAttribute("video") Video video) {
		if(!checkRoles.hasRole("ROLE ADMIN")) return "home";
		Video thisVideo = videoService.findById(video.getId());	
		if(request.getParameter("removeVideo") != null) {
			
			
			fileUtils.removeFileFromStorage(uploadDir + "/" + thisVideo.getPath());
			
        	videoService.deleteById(video.getId());
        	System.out.println("Video Deleted From Database !");
	        
		}else {
			
			videoService.updateVideo(thisVideo, video);
			videoService.save(thisVideo);
		}
		return "redirect:/secured/myvideos";
	}
	
	
}




/*
 * 
 * 
 * //		System.out.println(checkRoles.hasRole("ADMIN"));
//		System.out.println(checkRoles.hasRole("USER"));
//		System.out.println(checkRoles.hasRole("ROLE_ADMIN"));
		System.out.println(checkRoles.hasRole("ROLE ADMIN"));
//		System.out.println(request.isUserInRole("ROLE USER"));
//		System.out.println(request.isUserInRole("ROLE_USER"));
//		System.out.println(authentication.getAuthorities().contains(new SimpleGrantedAuthority("USER")));
//		System.out.println(request.getUserPrincipal());
//		System.out.println(session);
//		System.out.println(modelMap.values());
//		if(principle.)
//		System.out.println("NAME OF USER : " + principle.toString());
//		System.out.println("AUTHENTICATION : " + authentication);
//		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//		System.out.println("AUTH : " + auth.getPrincipal().toString());
//		System.out.println("AUTH : " + auth.getDetails());
//		System.out.println("AUTH : " + auth.getCredentials());
 * 
 * 
 * 
 * 
 * */


