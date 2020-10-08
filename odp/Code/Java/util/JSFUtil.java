package util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

public enum JSFUtil {
	;

	public static final int SCOPE_APPLICATION = 0;
	public static final int SCOPE_SESSION = 1;

	public static Object resolveVariable(String variable) {
		try {
			return FacesContext.getCurrentInstance().getApplication().getVariableResolver()
					.resolveVariable(FacesContext.getCurrentInstance(), variable);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getContextPath() {
		try {
			return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static void recreateBean(Integer scope, String bean_name, Object bean) {
		FacesContext context = FacesContext.getCurrentInstance();
		switch (scope) {
		case SCOPE_APPLICATION: {
			context.getExternalContext().getApplicationMap().remove(bean_name);
			context.getExternalContext().getApplicationMap().put(bean_name, bean);
			break;
		}
		case SCOPE_SESSION: {
			context.getExternalContext().getSessionMap().remove(bean_name);
			context.getExternalContext().getSessionMap().put(bean_name, bean);
			break;

		}
		}

	}

	public static void addError(String message) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(message, new FacesMessage(message));
		
	}

}
