package home.abel.photohub.webauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import home.abel.photohub.web.HeaderBuilderService;
import home.abel.photohub.web.TokenAuthenticationService;
import home.abel.photohub.webauth.DBUserDetailsService;
//import org.springframework.security.core.userdetails.UserDetailsService;

@EnableWebSecurity
@Configuration
@Order(1)
public class StatelessAuthenticationSecurityConfig extends WebSecurityConfigurerAdapter {

//	@Autowired
//	private UserDetailsService userDetailsService;

	@Autowired
	private UserDetailsService  userDetailsService;
	
	@Autowired
	private TokenAuthenticationService tokenAuthenticationService;
	
	@Autowired
	HeaderBuilderService headerBuilderService;

	public StatelessAuthenticationSecurityConfig() {
		super(true);
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService()).passwordEncoder(new BCryptPasswordEncoder());
	}	
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.anonymous().and()
				.exceptionHandling().and()
				.servletApi().and()	
				.authorizeRequests()
				
//				.headers().cacheControl().and()  - ????

				//allow anonymous resource requests
				.antMatchers("/").permitAll()
				.antMatchers("/favicon.ico").permitAll()
				.antMatchers("/resources/**").permitAll()
				.antMatchers("/css/**").permitAll()
				.antMatchers("/fonts/**").permitAll()
				.antMatchers("/js/**").permitAll()
				.antMatchers("/img/**").permitAll()
				.antMatchers("/*.html").permitAll()
				.antMatchers("/*.json").permitAll()
				.antMatchers("/api/login","/api/login/**").permitAll()
				.antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
				
				//allow anonymous POSTs to login
//				.antMatchers(HttpMethod.GET, "/api/checkLogin").permitAll()
//				.antMatchers(HttpMethod.POST, "/api/login").permitAll()
//				.antMatchers(HttpMethod.GET, "/api/checkLogin").permitAll()
				
				//allow anonymous GETs to API
				//.antMatchers(HttpMethod.GET, "/api/**").permitAll()
				//.antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
				//.antMatchers("/api/**").hasRole("USER")

				//define Admin only Area
				//.antMatchers("/admin/**").access("hasRole('ADMIN') and hasRole('DBA')") 
				.antMatchers("/admin/**","/api/admin/**").hasRole("ADMIN")
				
				//all other request need to be authenticated
				.anyRequest().authenticated().and()
				
				// custom JSON based authentication by POST of {"username":"<name>","password":"<password>"} which sets the token header upon authentication
//				.addFilterBefore(
//						new StatelessLoginFilter("/api/login",
//								tokenAuthenticationService,
//								userDetailsService,
//								authenticationManager()
//						), UsernamePasswordAuthenticationFilter.class)

				// custom Token based authentication based on the header previously given to the client
				.addFilterBefore(
						new StatelessAuthenticationFilter(tokenAuthenticationService,headerBuilderService),
						UsernamePasswordAuthenticationFilter.class);
	}
	
//	@Bean
//	public TokenAuthenticationService tokenAuthenticationService() {
//		return new TokenAuthenticationService();
//	}
		
	@Bean(name = "UserDetailsService")
	// any or no name specified is allowed
	@Override
	public UserDetailsService userDetailsServiceBean() throws Exception {
		//return super.userDetailsServiceBean();
		return new DBUserDetailsService();
	}
	 
	@Override
	protected UserDetailsService userDetailsService() {
		return userDetailsService;
	}
	
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

}
