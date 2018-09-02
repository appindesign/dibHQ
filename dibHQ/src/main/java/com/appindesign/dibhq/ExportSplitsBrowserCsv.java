package com.appindesign.dibhq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.appindesign.diblibrary.Competitor;
import com.appindesign.diblibrary.ControlCard;
import com.appindesign.diblibrary.Msg;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

public class ExportSplitsBrowserCsv {

	ExportSplitsBrowserCsv( Context context ) 
	{
        //SplitsBrowser "null" time
        final String sNullTime = "00:00";
        long lLastGoodTimestamp = Msg.INVALID_TIME;
        long lSplitTime = 0l;

		//Course and result declarations.
		ListOfCourses listOfCourses;
		Competitor competitor;
		ControlCard controlCard;
		String sSplitsBrowserText = "";
		
		//Data store declarations.
		Cursor cursor;
		String sSqlWhere = "";
		
		final SimpleDateFormat mSdf_Local_HH_mm = new SimpleDateFormat( "HH:mm", Locale.getDefault() );
		final SimpleDateFormat mSdf_UTC_ss = new SimpleDateFormat( ":ss" );
		mSdf_UTC_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		long splitMinutes;
		
		//Make a list of the courses.
		listOfCourses = new ListOfCourses( context );
		
		//For each course in the list, find each result.
		for ( int i=0; i < listOfCourses.getNumberOfCourses(); i++ ) {
			
			sSqlWhere = "COL_NAME = \'" + listOfCourses.getName(i) + "\'";
			cursor = context.getContentResolver().query( DibHqLocalStore.CONTENT_URI, null, sSqlWhere, null, null );

			if ( cursor.moveToFirst() )
			{
				competitor = new Competitor( cursor );
				controlCard = new ControlCard( cursor );

				//Course name and number of controls.
				sSplitsBrowserText += controlCard.getName() + ",";
				sSplitsBrowserText += controlCard.getNumberOfControls() + "\r\n";
				
				//Competitors and their times.
				do {
					controlCard = new ControlCard( cursor );					
					if ( controlCard.getNumberOfControls() != 0 ) {

						//Competitor.
						competitor = new Competitor( cursor );
						sSplitsBrowserText += competitor.getForename() + ",";
						sSplitsBrowserText += competitor.getSurname() + ",";
						sSplitsBrowserText += competitor.getClub() + ",";
						
//						//Start time.
//						if ( controlCard.getStartTimestamp() == Msg.INVALID_TIME ) {
//							sSplitsBrowserText += context.getString( R.string.mp );
//						}
//						else {
//							sSplitsBrowserText += mSdf_Local_HH_mm.format( controlCard.getStartTimestamp() );
//						}
//
//						//Split times (as mmm:ss).
//						for ( int j=1; j <= controlCard.getNumberOfControls()+1; j++ ) {
//							if ( controlCard.getSplitTime(j) != Msg.INVALID_TIME )
//							{
//								splitMinutes = ( controlCard.getSplitTime(j) )/1000/60;
//								sSplitsBrowserText += "," + splitMinutes + mSdf_UTC_ss.format( controlCard.getSplitTime(j) );
//							}
//							else
//							{
//								if ( controlCard.getTimestamp(j) == Msg.INVALID_TIME )
//									sSplitsBrowserText += "," + context.getString( R.string.mp );
//								else
//									sSplitsBrowserText += ",";
//							}
//						}

                        lLastGoodTimestamp = Msg.INVALID_TIME;

						//Start time.
                        if ( controlCard.getStartTimestamp() == Msg.INVALID_TIME ) {
//                            sSplitsBrowserText += sNullTime;
                            sSplitsBrowserText += context.getString( R.string.mp );
                        }
                        else {
                            lLastGoodTimestamp = controlCard.getStartTimestamp();
                            sSplitsBrowserText += mSdf_Local_HH_mm.format( lLastGoodTimestamp );
                        }

                        //Split times (as mmm:ss).
                        for ( int j=1; j <= controlCard.getNumberOfControls()+1; j++ ) {

                            if ( controlCard.getTimestamp(j) == Msg.INVALID_TIME ) {
//                                sSplitsBrowserText += "," + sNullTime;
                                sSplitsBrowserText += "," + context.getString( R.string.mp );
                            }
                            else {
                                if ( lLastGoodTimestamp == Msg.INVALID_TIME ) {
//                                    sSplitsBrowserText += "," + sNullTime;
                                    sSplitsBrowserText += "," + context.getString( R.string.noSplit );
                                }
                                else {
                                    lSplitTime = controlCard.getTimestamp(j) - lLastGoodTimestamp;
                                    splitMinutes = lSplitTime/1000/60;
                                    sSplitsBrowserText += "," + splitMinutes + mSdf_UTC_ss.format( lSplitTime );
                                }
                                lLastGoodTimestamp = controlCard.getTimestamp(j);
                            }
                        }
						
						//Finish with a new line.
						sSplitsBrowserText += "\r\n";
					}
				}
				while ( cursor.moveToNext() );
				cursor.close();
				
				//Add a blank line before the next course.
				sSplitsBrowserText += "\r\n";
			}
		}
		writeStringToFile( context, sSplitsBrowserText );
	}
	
	private void writeStringToFile( Context context, String sSplitsBrowserText )
	{
		// File variables.
		File file = null;
		StorageLocation storageLocation;
		FileOutputStream fileOutputStream = null;
		OutputStreamWriter outputStreamWriter = null;
		
		// See if you can create a file.
		storageLocation = new StorageLocation( context, context.getString( R.string.splitsbrowserFilename ) );
		file = storageLocation.getFile();
		
		if ( file != null )
		{
			file.delete();
			
			try 
			{	//TODO 8K bytes buffer size on OutputStreamWriter...may not be enough? Unlikely.		
				fileOutputStream = new FileOutputStream( file );
				outputStreamWriter = new OutputStreamWriter( fileOutputStream );
				outputStreamWriter.write( sSplitsBrowserText );
				outputStreamWriter.flush();
				if ( fileOutputStream != null ) fileOutputStream.close();
				if ( outputStreamWriter != null ) outputStreamWriter.close();
			}
			catch ( Exception e ) {	
				String sMessage = context.getString( R.string.unable_to_save ) + context.getString( R.string.splitsbrowserFilename );
				Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
			}
		}
	}
	
}