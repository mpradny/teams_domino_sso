package to.pris.teams;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.ibm.commons.util.StringUtil;

/**
 * @author Martin Pradny, Pristo (https://pris.to)
 */
public class TokenUtils {

	private static final Logger logger = Logger.getLogger(TokenUtils.class.getName());

	private String token;

	private DecodedJWT decoded = null;

	public TokenUtils(String token) {
		this.token = token;
	}

	public DecodedJWT decodeToken() {
		if (decoded == null) {
			decoded = JWT.decode(token);
		}

		return decoded;
	}

	private JWTVerifier getVerifier(String keyId) throws SSOException {
		AppConfBean appConf = AppConfBean.get();
		HashMap<String, JWTVerifier> cache = appConf.getVerifierCache();

		JWTVerifier verifier = cache.get(keyId);
		if (verifier == null) {
			try {
				JwkProvider provider = new UrlJwkProvider(
						new URL("https://login.microsoftonline.com/common/discovery/keys"));
				Jwk jwk = provider.get(keyId);
				Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
				verifier = JWT.require(algorithm).acceptLeeway(1).build();

			} catch (Exception e) {
				e.printStackTrace();
				logger.log(Level.SEVERE, e.toString(), e);
			}

			if (verifier != null) {
				cache.put(keyId, verifier);
			} else {
				throw new SSOException("Unable to build token verifier");
			}
		}

		return verifier;
	}

	public boolean validateToken() throws SSOException {

		DecodedJWT jwt = decodeToken();
		logger.fine("Token key id: " + jwt.getKeyId());

		JWTVerifier verifier = getVerifier(jwt.getKeyId());

		try {
			verifier.verify(token);
		} catch (SignatureVerificationException e) {

			logger.log(Level.SEVERE, e.toString(), e);
			throw new SSOException("Token verification failed");
		}

		AppConfBean appConf = AppConfBean.get();
		String resourceURL = appConf.getResourceURL();
		if (!StringUtil.isEmpty(resourceURL)) {
			String tokenAud = jwt.getAudience().get(0);
			if (!resourceURL.equals(tokenAud)){
				throw new SSOException("Token audience check failed");
			}
		}
		return true;
	}

	public String getTokenUser() {
		DecodedJWT jwt = decodeToken();
		Map<String, Claim> claims = jwt.getClaims();
		return claims.get("upn").asString();
	}
}
