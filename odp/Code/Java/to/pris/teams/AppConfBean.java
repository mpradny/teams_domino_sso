package to.pris.teams;

import java.io.Serializable;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.auth0.jwt.interfaces.JWTVerifier;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import util.JSFUtil;

/**
 * @author Martin Pradny, Pristo (https://pris.to)
 */
public class AppConfBean implements Serializable {

	private static final Logger logger = Logger.getLogger(AppConfBean.class.getName());
	
	private static final long serialVersionUID = 1L;
	public static final String BEAN_NAME = "appConf";
	private static boolean devMode=false;  //used to disable some caching and work around issues with design changes;
	
	
	private transient HashMap<String,JWTVerifier> verifierCache = new HashMap<String,JWTVerifier>();
	private String domain;
	private boolean initialized = false;

	private String sameSite;

	private String ltpaConfig;

	private String resourceURL;
	
	
	
	public String getSameSite() {
		init();
		return sameSite;
	}

	public String getLtpaConfig() {
		init();
		return ltpaConfig;
	}

	public HashMap<String, JWTVerifier> getVerifierCache() {
		return verifierCache;
	}

	public static void setDevMode(boolean devMode) {
		AppConfBean.devMode = devMode;
	}

	public static boolean isDevMode() {
		return AppConfBean.devMode;
	}
	
	public static AppConfBean get(){
		try{
			AppConfBean appConf=(AppConfBean) JSFUtil.resolveVariable(BEAN_NAME);
			if (appConf==null){
				throw new Exception("Bean not found");
			}
			return appConf;
		}catch(Exception e){
			if (isDevMode()){
				//probably design changes and bean throws class cast exception (or something weird
				AppConfBean appConf=new AppConfBean();
				JSFUtil.recreateBean(JSFUtil.SCOPE_APPLICATION,BEAN_NAME,appConf);
				return appConf;

			}
			e.printStackTrace();
			return null;
		}
	}

	public String getDomain() {
		init();
		
		return domain;		
	}
	
	public String getResourceURL() {
		init();
		return resourceURL;
	}

	private void init() {
		if (!initialized) {
			
			synchronized(this) {
				if (!initialized) {
					try {
						Session sas = ExtLibUtil.getCurrentSessionAsSigner();
						Database db = sas.getCurrentDatabase();
						Document profile = db.getProfileDocument("(Profile)", "");
						ltpaConfig = profile.getItemValueString("LtpaConfig");
						domain = profile.getItemValueString("Domain");
						sameSite = profile.getItemValueString("SameSite");
						if (sameSite.isEmpty()) {
							sameSite = "None";
						}
						resourceURL = profile.getItemValueString("ResourceURL");
					} catch (NotesException e) {
						logger.log(Level.SEVERE,e.toString(),e);
					}
					
					initialized = true;
				}
			}			
		}		
	}



}
