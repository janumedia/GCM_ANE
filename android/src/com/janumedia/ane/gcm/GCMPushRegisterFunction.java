package com.janumedia.ane.gcm;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREInvalidObjectException;
import com.adobe.fre.FREObject;
import com.adobe.fre.FRETypeMismatchException;
import com.adobe.fre.FREWrongThreadException;
import com.google.android.gcm.GCMRegistrar;

public class GCMPushRegisterFunction implements FREFunction {

	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		
		String message = "ok";
		
		String senderID = "";
		
		try {
			senderID = args[0].getAsString();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (FRETypeMismatchException e) {
			e.printStackTrace();
		} catch (FREInvalidObjectException e) {
			e.printStackTrace();
		} catch (FREWrongThreadException e) {
			e.printStackTrace();
		}
		
		//message = "registering senderID: " + senderID;
		
		GCMRegistrar.checkDevice(context.getActivity().getApplication());
		
		String regID = GCMRegistrar.getRegistrationId(context.getActivity().getApplication());
		
		if (regID.equals(""))
		{
			GCMRegistrar.register(context.getActivity().getApplication(), new String[] { senderID });
			message = "registering senderID: " + senderID;
		} else 
		{
			message = "registrationID:" + regID;
		}
		
		//message = "registering senderID: " + senderID + ", regID: " + regID;
		
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
