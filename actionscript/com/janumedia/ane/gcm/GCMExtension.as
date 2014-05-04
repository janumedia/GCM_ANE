package com.janumedia.ane.gcm
{
	import flash.events.EventDispatcher;
	import flash.events.StatusEvent;
	import flash.external.ExtensionContext;
	
	public class GCMExtension extends EventDispatcher
	{
		public static const VERSION:String = "1.0";
		public static const REGISTERED:String = "registered";
        public static const MESSAGE:String = "message";
        public static const UNREGISTERED:String = "unregistered";
        public static const ERROR:String = "error";
        public static const RECOVERABLE_ERROR:String = "recoverableError";
        public static const NO_MESSAGE:String = "false";
				
		private var _context:ExtensionContext;
		
		public function GCMExtension ()
		{
			if (!_context)
			{
				_context = ExtensionContext.createExtensionContext("com.janumedia.ane.gcm", null);
				_context.addEventListener(StatusEvent.STATUS, onContextStatus, false, 0, true);
			}
		}
		
		public function register (senderID:String) : String
		{
			var str:String = "";

			if (_context)
			{
				str = String (_context.call("register", senderID));
			}
			
			return str;
		}
		
		public function unregister () : String
		{
			var str:String = "";
			if (_context) str = String (_context.call("unregister"));
			
			return str;
		}
		
		public function checkPendingPayload () : String
		{
			var str:String = "";
			if (_context) str = String (_context.call("checkPendingPayload"));
			
			return str;
		}
		
		private function onContextStatus(e:StatusEvent):void 
		{
			var gcmEvent:GCMEvent;
			
			trace(this, "Status: " + e.level);
            trace(this, "Code: " + e.code);
			
			switch (e.code)
			{
				case REGISTERED:
					gcmEvent = new GCMEvent(GCMEvent.REGISTERED);
					gcmEvent.deviceRegistrationID = e.level;
					break;
				case UNREGISTERED:
					gcmEvent = new GCMEvent(GCMEvent.UNREGISTERED);
					gcmEvent.deviceRegistrationID = e.level;
					break;
				case MESSAGE:
					gcmEvent = new GCMEvent(GCMEvent.MESSAGE);
					gcmEvent.message = e.level;
					break;
				case ERROR:
					gcmEvent = new GCMEvent(GCMEvent.ERROR);
					gcmEvent.errorCode = e.level;
					break;
				case RECOVERABLE_ERROR:
					gcmEvent = new GCMEvent(GCMEvent.RECOVERABLE_ERROR);
					gcmEvent.errorCode = e.level;
					break;
				default:
					break;
			}
			
			if (gcmEvent) 
			{
				trace (this, "dispatchEvent", gcmEvent.type);
				dispatchEvent(gcmEvent);
			}
			
		}
	}
}