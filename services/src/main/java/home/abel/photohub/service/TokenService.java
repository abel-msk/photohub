package home.abel.photohub.service;

import home.abel.photohub.model.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public final class TokenService {

	final Logger logger = LoggerFactory.getLogger(TokenService.class);

	
	private static final String HMAC_ALGO = "HmacSHA256";
	private static final String SEPARATOR = ".";
	private static final String SEPARATOR_SPLITTER = "\\.";

	public static final long EXPIRES_DAY = 1000 * 60 * 60 * 24;
	
	private  Mac hmac;
	
    @Autowired
    Environment env;
	
    /****************************************************************
     * 
     */
	public TokenService() {
		
	}
	
	public TokenService(byte[] secretKey) {
		prepareKey(secretKey);
	}
    
    /*****************************************************************
     * 
     * Prepare token key for use with Token processing, if it was not done with object creation
     * 
     */
	@PostConstruct
	public void Init() {
		if (hmac == null) {
			String tokenStr =  env.getProperty("token.secret");
			if (tokenStr == null) {
				tokenStr = "9SyECk96oDsTmXfogIieDI0cD/8FpnojlYSUJT5U9I/FGVmBz5oskmjOR8cbXTvoPjX+Pq/T/b1PqpHX0lYm0oCBjXWICA==";
			}
			byte[] secretKey = DatatypeConverter.parseBase64Binary(tokenStr);
			prepareKey(secretKey);
		}
	}
	
	/*****************************************************************
	 * 
	 *  Init HMAC key 
	 *  
	 * @param secretKey
	 */
	private void prepareKey(byte[] secretKey) {
		try {
			hmac = Mac.getInstance(HMAC_ALGO);
			hmac.init(new SecretKeySpec(secretKey, HMAC_ALGO));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("failed to initialize HMAC: " + e.getMessage(), e);
		}
		catch (InvalidKeyException e) {
			throw new IllegalStateException("failed to initialize HMAC: " + e.getMessage(), e);
		}
	}
	
	
	
	/*****************************************************************
	 * 
	 * 	Get User detail info from Token
	 * 
	 * @param token
	 * @return
	 */
	public User parseUserFromToken(String token) {
		final String[] parts = token.split(SEPARATOR_SPLITTER);
		if (parts.length == 2 && parts[0].length() > 0 && parts[1].length() > 0) {
			try {
				final byte[] userBytes = fromBase64(parts[0]);
				final byte[] hash = fromBase64(parts[1]);

				boolean validHash = Arrays.equals(createHmac(userBytes), hash);
				if (validHash) {
					final User user = fromJSON(userBytes);
					if (new Date().getTime() < user.getExpires()) {
						return user;
					}
					else {
						logger.debug("User token has expires.");
					}
				}
			} catch (IllegalArgumentException e) {
				//log tempering attempt here
			}
		}
		return null;
	}

	/*****************************************************************
	 * 
	 * 	Prepare token for pass to User
	 * 
	 * @param token
	 * @return
	 */
	public String createTokenForUser(User user) {
		byte[] userBytes = toJSON(user);		
		byte[] hash = createHmac(userBytes);
		final StringBuilder sb = new StringBuilder(170);
		sb.append(toBase64(userBytes));
		sb.append(SEPARATOR);
		sb.append(toBase64(hash));
		return sb.toString();
	}

	
	
	private User fromJSON(final byte[] userBytes) {
		try {
			return new ObjectMapper().readValue(new ByteArrayInputStream(userBytes), User.class);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private byte[] toJSON(User user) {
		try {
			String userJsonViewStr= new ObjectMapper().writeValueAsString(user);
			//logger.debug("user as json view = " + userJsonViewStr);
			return  userJsonViewStr.getBytes();
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private String toBase64(byte[] content) {
		return DatatypeConverter.printBase64Binary(content);
	}

	private byte[] fromBase64(String content) {
		return DatatypeConverter.parseBase64Binary(content);
	}

	// synchronized to guard internal hmac object
	private synchronized byte[] createHmac(byte[] content) {
		return hmac.doFinal(content);
	}

}
