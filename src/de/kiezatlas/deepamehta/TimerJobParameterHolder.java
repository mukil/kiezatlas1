package de.kiezatlas.deepamehta;

import java.util.HashMap;
import java.util.Map;

public class TimerJobParameterHolder {

	private static TimerJobParameterHolder instance;
	
	private Map<String, Object> parameters;
	
	private TimerJobParameterHolder() {
		parameters = new HashMap<String, Object>();
	}
	
	public static TimerJobParameterHolder getInstance() {
		if (instance == null) {
			instance = new TimerJobParameterHolder();
		}
		return instance;
	}

  public void clearReferences() {
		if (parameters != null) {
			parameters = new HashMap<String, Object>();
		}
    System.out.println("DEBUG: TimerJobParameterHolder.clearedReferences()... " + parameters.values());
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}
}
