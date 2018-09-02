package com.appindesign.dibhq;

import com.appindesign.diblibrary.Msg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * ReceiverOfSms looks at incoming SMSs.
 * The SMS is extracted and split(",") into an array.
 * The first item is tested to see if it is the dib code word.
 * If it is, the array is broadcast to dibHq's result receiver.
 */

public class ReceiverOfSms extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent ) 
    {
        SmsMessage[] smsMessagesArray = null;
        String[] sSmsArray = null;
        String smsText = "";
        Bundle bundle = intent.getExtras();
        
		//---get the SMS message passed in---           
        if ( bundle != null )
        {
            //Retrieve the SMS messages received.
            Object[] pdus = (Object[]) bundle.get( "pdus" );
            smsMessagesArray = new SmsMessage[ pdus.length ];            
            for ( int i=0; i<smsMessagesArray.length; i++ )
            {
                smsMessagesArray[i] = SmsMessage.createFromPdu( (byte[]) pdus[i] );
                smsText += smsMessagesArray[i].getMessageBody().toString();
            }
            sSmsArray = smsText.split( "," );
        }

        //See if it is a dib SMS. It is if the first word is msDIB_CODE.
        if ( ( sSmsArray != null ) && sSmsArray[0].equals( Msg.CODE_WORD ) )
        {
			//Send a broadcast to the result receiver.
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction( DibHqMain.msDIB_STORE_RESULT_ACTION );
			broadcastIntent.putExtra( "result", sSmsArray );
			context.sendBroadcast( broadcastIntent );       	
        }      
    }                         
}
