package to.pris.teams;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import com.developi.openntf.LtpaGenerator;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import util.JSFUtil;

/**
 * @author Martin Pradny, Pristo (https://pris.to)
 */
public class Authenticator {

	private static final Logger logger = Logger.getLogger(Authenticator.class.getName());
	private AppConfBean appConf;
	
	
	public Authenticator() {
		appConf = AppConfBean.get();
	}
	
	public void authenticate(String token, String destination) {
		TokenUtils tokenUtils = new TokenUtils(token);
		
		try {
			tokenUtils.validateToken();
			String user = tokenUtils.getTokenUser();
			
			String notesUser =  getNotesUser(user);
		
			if (notesUser!=null) {
				String ltpaToken = generateLtpaToken(notesUser);
				if (ltpaToken!=null) {
					redirectWithToken(ltpaToken, destination);
				} else {
					throw new SSOException("Unable to generate LTPA token");	
				}
			}
			else
			{
				throw new SSOException("No Notes user found for " + user);			
			}
		} catch (SSOException e) {
			logger.log(Level.SEVERE,e.toString(),e);
			JSFUtil.addError(e.toString());
		}
	}
	
	@SuppressWarnings("rawtypes")
	private String getNotesUser(String user) {
		
		Session session = ExtLibUtil.getCurrentSessionAsSigner();
		try {
			Database nab = (Database) session.getAddressBooks().firstElement();
			if (nab.open()) {
				View v = nab.getView("($Users)");
				ViewEntry ve = v.getEntryByKey(user,true);
				
				if (ve!=null) {
					Object users = ve.getColumnValues().get(1);
					String notesUser;
					if (users instanceof String) {
						notesUser=user;
						
					}else if(users instanceof Vector) {
						notesUser = ((Vector) users).firstElement().toString();
					} else {
						notesUser = users.toString();
					}
					logger.fine("Found Notes user:" + notesUser);
					ve.recycle();
					return notesUser;
				}
				v.recycle();
			}
		} catch (NotesException e) {
			logger.log(Level.SEVERE,e.toString(),e);
		}
		return null;
	}

	private void redirectWithToken(String token, String destination) {
		
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		response.setHeader("Set-Cookie",
				"LtpaToken=" + token + "; domain=" + appConf.getDomain() +"; path=/; Secure; HttpOnly; SameSite=" + appConf.getSameSite());
		try {
			facesContext.getExternalContext().redirect(destination);
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	private String generateLtpaToken(String user) {
		// 
		Session sessionAsSigner = ExtLibUtil.getCurrentSessionAsSigner();
		LtpaGenerator ltpa = new LtpaGenerator();
		try {
			ltpa.initByConfiguration(sessionAsSigner, appConf.getLtpaConfig());
			String token = ltpa.generateLtpaToken(user);
			return token;
		} catch (Exception e) { 			
			logger.log(Level.SEVERE,e.toString(),e);
		}
		
		return null;	
	}

}
