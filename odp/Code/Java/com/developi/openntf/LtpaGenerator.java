/**
 * LtpaToken Generator V1.1
 * 
 * This Java class generates a valid LtpaToken valid for any user name.
 * 
 * To use it on SSJS:
 * -------------------
 *  importPackage(com.developi.openntf);
 *  var ltpa:LtpaGenerator=new LtpaGenerator();
 *  ltpa.initByConfiguration(sessionAsSigner, "Developi:LtpaToken");
 *  token=ltpa.generateLtpaToken("CN=Serdar Basegmez/O=developi");
 * 
 * To use the token (make sure replace '.developi.info' with your SSO domain):
 * -------------------------------------------------------------------------
 *  response=facesContext.getExternalContext().getResponse();
 *  response.setHeader("Set-Cookie", "LtpaToken=" + token + "; domain=.developi.info; path=/");
 * facesContext.getExternalContext().redirect(someUrl);
 * 
 * 1. "Developi:LtpaToken" is the SSO configuration key. If you are using Internet site configuration,  it will be 
 *     "Organization:TokenName". Otherwise, it will be "TokenName" only. You may check "($WebSSOConfigs)"
 *     view in the names.nsf database.
 * 2. sessionAsSigner should be given as parameter to the initByConfiguration method.
 * 3. The signer of the database design should be listed as 'Owner' or 'Administrator' in the SSO configuration.
 * 4. Current version only supports Domino keys. Tokens imported from Websphere will not generate valid tokens.
 * 
 * Important Note:
 * You will see "LMBCS" encoding below. This is because of that Domino encodes user names in LMBCS charset.
 * As long as you use standard ANSI characters, it's OK. However if you use other languages (like Turkish) in
 * user names, it will be encoded in default charset (ISO-8859-1). Normally, Domino JVM does not support LMBCS
 * encoding. So you have to install a supporting library. I have found ICU (International Components for Unicode) library.
 * However, it cannot be attached into NSF. So you have to install it into Domino JVM. To do this;
 * 
 *  - Go to ICU Project site (http://www.icu-project.org)
 *  - Download "icu4j-49_1.jar" and "icu4j-charset-49_1.jar" (or latest versions)
 *  - Put those files into "{Domino Program Folder}\jvm\lib\ext"
 *  - Restart your HTTP task
 * 
 * This will install ICU library into your server. This library is licensed under X-License and can be used commercially.
 * I didn't try but it can also be installed via OSGi plugin. Let me know if you do it :)
 * Direct link for download: http://apps.icu-project.org/icu-jsp/downloadPage.jsp?ver=49.1&base=j&svn=release-49-1
 * 
 */
 
package com.developi.openntf;
 
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.GregorianCalendar;
 
import javax.xml.bind.DatatypeConverter;
 
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
 
/*
 * @author Serdar Basegmez, Developi (http://lotusnotus.com/en)
 */
 
public class LtpaGenerator {
 
  public final String NAMESDB="names.nsf";
  public final String SSOVIEW="($WebSSOConfigs)";
  public final String SSO_DOMINO_SECRETFIELD="LTPA_DominoSecret";
  public final String SSO_DOMINO_DURATIONFIELD="LTPA_TokenExpiration";
   
  private boolean ready=false;
 
  private int duration=300;
  private String ltpaSecret="";
   
  public LtpaGenerator() {
  }
 
  public LtpaGenerator(String ltpaSecret) {
    setLtpaSecret(ltpaSecret);
  }
 
  public LtpaGenerator(String ltpaSecret, int duration) {
    setLtpaSecret(ltpaSecret);
    setDuration(duration);
  }
   
  public void initByConfiguration(Session session, String configName) throws Exception {
    Database dbNames=null;
    View ssoView=null;
    Document ssoDoc=null;
     
    try {
      String currentServer=session.getCurrentDatabase().getServer();
      dbNames=session.getDatabase(currentServer, NAMESDB, false);
      ssoView=dbNames.getView(SSOVIEW);
      ssoDoc=ssoView.getDocumentByKey(configName, true);
      if(ssoDoc==null) {
        throw new IllegalArgumentException("Unable to find SSO configuration with the given configName.");
      }
       
      setLtpaSecret(ssoDoc.getItemValueString(SSO_DOMINO_SECRETFIELD));
      setDuration(ssoDoc.getItemValueInteger(SSO_DOMINO_DURATIONFIELD));
       
    } catch (NotesException ex) {
      throw new Exception("Notes Error: "+ex);
    } finally {
      try {
        if(dbNames!=null) dbNames.recycle();
        if(ssoView!=null) ssoView.recycle();
        if(ssoDoc!=null) ssoDoc.recycle();        
      } catch(NotesException exc) {
        //ignore
      }
    }
  }
   
  public String generateLtpaToken(String userName) {
    if(!isReady()) {
      throw new IllegalStateException("LtpaGenerator is not ready.");
    }
     
    MessageDigest sha1 = null;
 
    GregorianCalendar creationDate=new GregorianCalendar();
    GregorianCalendar expiringDate=new GregorianCalendar();
 
    byte[] userNameArray=userName.getBytes();
     
    expiringDate.add(GregorianCalendar.MINUTE, duration);
     
    try {
      sha1 = MessageDigest.getInstance( "SHA-1" );
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace(System.err);
    }
         
    byte[] secretDecoded=DatatypeConverter.parseBase64Binary(ltpaSecret);
 
    // Look at important notes above...
    try {
      if(Charset.isSupported("LMBCS")) {
        userNameArray=userName.getBytes("LMBCS");
      }
    } catch (UnsupportedEncodingException e) {
      // Not supposed to fall here.
    }
       
    byte[] tokenBase=concatBytes(("\000\001\002\003"+getHexRep(creationDate)+getHexRep(expiringDate)).getBytes(), userNameArray);
     
    byte[] digest=sha1.digest(concatBytes(tokenBase, secretDecoded));
   
    return DatatypeConverter.printBase64Binary(concatBytes(tokenBase, digest));
     
  }
 
  public static byte[] concatBytes(byte[] arr1, byte[] arr2) {
    byte[] result=Arrays.copyOf(arr1, arr1.length+arr2.length);
    System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
    return result; 
  }
   
  public static String getHexRep(GregorianCalendar date) {
    int timeVal=(int)(date.getTimeInMillis()/1000);
    String hex=Integer.toHexString(timeVal).toUpperCase();
     
    if(hex.length()>=8) {
      return hex; 
    } else {
      return String.format("%0"+(8-hex.length())+"d", 0)+hex;
    }
  }
 
  public void setDuration(int duration) {
    this.duration = duration;
  }
 
  public void setLtpaSecret(String ltpaSecret) {
    this.ltpaSecret = ltpaSecret;
    this.ready=true;
  }
 
  public boolean isReady() {
    return ready;
  }
 
}