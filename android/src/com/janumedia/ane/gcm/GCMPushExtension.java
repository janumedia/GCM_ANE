package com.janumedia.ane.gcm;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREExtension;

public class GCMPushExtension implements FREExtension {

	public static FREContext context;
	
	@Override
	public FREContext createContext(String arg0) {
		return context = new GCMPushContext();
	}

	@Override
	public void dispose() {
		context.dispose();
		context = null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

}
