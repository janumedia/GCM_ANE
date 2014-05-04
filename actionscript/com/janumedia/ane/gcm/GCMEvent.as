package com.janumedia.ane.gcm
{
	import flash.events.Event;
	
	public class GCMEvent extends Event
	{
		public static const REGISTERED:String = "GCMEvent.REGISTERED";
        public static const UNREGISTERED:String = "GCMEvent.UNREGISTERED";
        public static const MESSAGE:String = "GCMEvent.MESSAGE";
        public static const ERROR:String = "GCMEvent.ERROR";
        public static const RECOVERABLE_ERROR:String = "GCMEvent.RECOVERABLE_ERROR";
		
		public var deviceRegistrationID:String;
        public var message:String;
        public var errorCode:String;
		
		public function GCMEvent (type:String, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
		}
		
		override public function clone():Event 
		{
			var gcmEvent:GCMEvent = new GCMEvent(type, bubbles, cancelable);
			gcmEvent.deviceRegistrationID = deviceRegistrationID;
			gcmEvent.message = message;
			gcmEvent.errorCode = errorCode;
			
			return gcmEvent;
		}
	}
}