package com.janumedia.ane.gcm;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.google.android.gcm.GCMRegistrar;

public class GCMPushUnregisterFunction implements FREFunction {

	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		
		String message = "Device not registered with GCM";
		
		GCMRegistrar.checkDevice(context.getActivity().getApplication());
		
		String regID = GCMRegistrar.getRegistrationId(context.getActivity().getApplication());
		
		if(!regID.equals(""))
		{
			GCMRegistrar.unregister(context.getActivity().getApplication());
			message = "Unregistering device from GCM";
		}
		
		try
		{
			return FREObject.newObject(message);
		} catch (FREWrongThreadException e)
		{
			e.printStackTrace();
		}
				
		return null;
	}

}
