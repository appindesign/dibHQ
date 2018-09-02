package com.appindesign.dibhq;

import com.appindesign.diblibrary.Msg;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * ReceiverOfResults watches for broadcasts of results from ReceiverOfSms.
 * It stores results in dibHq's local store.
 * Then it broadcasts an action to update the dibHq results slip.
 */

public class ReceiverOfResults extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent ) 
    {
        String[] sResultArray = null;
        ContentValues localStoreValues = new ContentValues(); 
        Bundle bundle = intent.getExtras();
                
        if ( bundle != null )
        {
        	sResultArray = ( String[] ) bundle.get( "result" );
        	
        	//Competitor
    		localStoreValues.put( "COL_FORENAME", 		sResultArray[Msg.FORENAME] );
    		localStoreValues.put( "COL_SURNAME", 		sResultArray[Msg.SURNAME] );
    		localStoreValues.put( "COL_CLUB", 			sResultArray[Msg.CLUB] );
    		localStoreValues.put( "COL_CLASSIFICATION", sResultArray[Msg.CLASSIFICATION] );
    		localStoreValues.put( "COL_IDENTIFIER", 	sResultArray[Msg.IDENTIFIER] );	
    		
    		//Course name and length.
    		localStoreValues.put( "COL_NAME", 			sResultArray[Msg.NAME] );  		
	    	localStoreValues.put( "COL_LENGTH",			sResultArray[Msg.LENGTH] );
    		
    		//Result status and time.
    		localStoreValues.put( "COL_STATUS", 		sResultArray[Msg.STATUS] );
    		localStoreValues.put( "COL_OLD_TIME", 		sResultArray[Msg.OLD_TIME] );

    		//long format time (only available after the first release of dib).
    		if ( sResultArray.length > Msg.TIME ) {
    			localStoreValues.put( "COL_TIME", Msg.TIME_DIVISOR * Long.parseLong( sResultArray[Msg.TIME] ) );
    		}
    		
    		//Codes and their timestamps (only available in long message).
    		if ( sResultArray.length >= Msg.TIMESTAMPS ) {
	    		localStoreValues.put( "COL_CODES", 				sResultArray[Msg.CODES] );
	    		localStoreValues.put( "COL_SPLIT_TIMESTAMPS", 	sResultArray[Msg.TIMESTAMPS] );   		
	    		}
    		else {
        		localStoreValues.put( "COL_SPLIT_TIMESTAMPS", 	"No Splits" ); 
    		}			
    		
    		context.getContentResolver().insert( DibHqLocalStore.CONTENT_URI, localStoreValues );
            
            //Send a broadcast to update the results list.
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction( DibHqMain.msDIB_UPDATE_RESULT_DISPLAY_ACTION );
            context.sendBroadcast( broadcastIntent );
        }
    }
}
