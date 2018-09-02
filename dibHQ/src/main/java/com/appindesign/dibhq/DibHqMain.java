package com.appindesign.dibhq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.appindesign.diblibrary.Competitor;
import com.appindesign.diblibrary.ControlCard;
import com.appindesign.diblibrary.Msg;
//import android.app.ActionBar;

/**
 * DibHqMain extends ActionBarActivity.
 * @author Appin
 *
 */
public class DibHqMain extends ActionBarActivity {

	//Sort parameters.
	int mnSortOrder;
	final int mnCOURSE_ORDER = 0;
	
	//Intent actions.
	static public final String msDIB_STORE_RESULT_ACTION = "DIB_STORE_RESULT_ACTION";
	static public final String msDIB_UPDATE_RESULT_DISPLAY_ACTION = "DIB_UPDATE_RESULT_DISPLAY_ACTION";
	
	//Result list to view adapter.
	ArrayList<HashMap<String, String>> mResultsList = new ArrayList<HashMap<String, String>>();
	SimpleAdapter mResultsAdapter;
	
	//Constants
	final int mQR_SCAN_ACTIVITY = 0;
	final SimpleDateFormat mSdf_Local_HH_mm = new SimpleDateFormat( "HH:mm", Locale.getDefault() );
	final SimpleDateFormat mSdf_UTC_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss" );
	
	//Receiver to respond to intents of "DIB_NFC_RESULT_ACTION".
	private BroadcastReceiver mResultReceiver = new BroadcastReceiver() 
	{
	    @Override
	    public void onReceive( Context context, Intent intent ) 
	    	{ refreshResultsDisplay(); };
	};
	
	//-------------------Lifecycle functions-------------
	// onCreate( Bundle savedInstanceState )
	// onPause()
	// onResume()
	
	/**
	 * 	Connects the result view to the results list and creates the navigation drop-down on the ActionBar.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dib_hq_main_layout);
		
		mSdf_UTC_HH_mm_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		
		//Create an adapter to connect the result list to the result views.
		final int[] nRESULT_VIEWS = new int[] { R.id.course, R.id.competitor, R.id.club, R.id.classification, R.id.time };
		mResultsAdapter = new SimpleAdapter( this, 
			mResultsList, 
			R.layout.result_slip_layout, 
			getResources().getStringArray( R.array.resultColumnTags ),
			nRESULT_VIEWS );
		
		//Connect the adapter to the results list view.
		ListView resultsView = (ListView) findViewById( R.id.resultList );
		resultsView.setAdapter( mResultsAdapter );

		//Initialise sort parameters.
		createSortNavigationDropDown(); 
	}
	
	/**
	 * 	Prepares the navigation drop-down (though onResume() sets the selected item in the drop-down).
	 */
	protected void createSortNavigationDropDown()
	{		
		//Replace the ActionBar title with a sort order drop-down list.
		getSupportActionBar().setDisplayShowTitleEnabled( false );
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		//Create an array adapter to populate the sort order drop-down list.
		final String[] sortListOptions = getResources().getStringArray( R.array.sortListOptions );
		SpinnerAdapter sortAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, sortListOptions);
		
		//Create a listener for the sort order drop-down list.
		OnNavigationListener navigationListener = new OnNavigationListener()
		
		{
			@Override
			public boolean onNavigationItemSelected( int itemPosition, long itemId ) 
			{
				mnSortOrder = itemPosition;
		 		refreshResultsDisplay();		
		 		return false;
			}
		};

		//Now join the listener and the adapter to make the navigation drop-down.
		getSupportActionBar().setListNavigationCallbacks( sortAdapter, navigationListener );		
	}

	/**
	 * Stores the sort order.
	 */
	protected void onPause()
	{
		unregisterReceiver( mResultReceiver );
		
		//Store the sort order.
		final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences( this );		
		final Editor prefsEditor = sharedPrefs.edit();
		prefsEditor.putInt( "SortOrder", mnSortOrder );
		prefsEditor.commit();
		
		super.onPause();
	}

	/**
	 * Registers the result receiver, sets the sort order and refreshes the result display.
	 */
	protected void onResume()
	{
		super.onResume();
		
		//Register the receiver of result notifications that are broadcast by the SMS receiver.
		IntentFilter resultIntentFilter = new IntentFilter( msDIB_UPDATE_RESULT_DISPLAY_ACTION );
		registerReceiver( mResultReceiver, resultIntentFilter );

		//Apply the preferred sort option, then refresh the results display.
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences( this );
		mnSortOrder = sharedPrefs.getInt( "SortOrder", 0 );
		if ( mnSortOrder > 5 ) mnSortOrder = 0;
		getSupportActionBar().setSelectedNavigationItem( mnSortOrder );
		
		refreshResultsDisplay();		
	}
	
	//------Functions to generate the results list.-------
	// refreshResultsDisplay()
	// getEventResults()
	// getListOfCourses()
	// getCourseResults( String sCourseName )

	/**
	 * Calls a method to make a results list and notifies the result view adapter of a change of data.
	 */
	private void refreshResultsDisplay()
	{
		makeEventResults();
		mResultsAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Makes a list of results for the event - and if you have sorted by course 
	 * includes a header line and blank footer line for each course.
	 */
	private void makeEventResults()
	{		
		ListOfCourses listOfCourses;
		HashMap<String, String> resultLine;
				
		mResultsList.clear();
		listOfCourses = new ListOfCourses( this );

		//If you are sorting by course name you need add a header and footer for each course.
		if ( mnSortOrder == mnCOURSE_ORDER )
		{
			//Iterate through each course.
			for( int i=0; i < listOfCourses.getNumberOfCourses(); i++ )
			{
				//Add a header.
				resultLine = new HashMap<String, String>();
				resultLine.put( "course", listOfCourses.getName(i) );
				mResultsList.add( resultLine );
				
				//Add the results for the course.		
				makeCourseResults( "COL_NAME = \'" + listOfCourses.getName(i) + "\'" );
				
				//Add a footer, if this isn't the last course.
				if ( i+1 < listOfCourses.getNumberOfCourses() )
				{
					resultLine = new HashMap<String, String>();
					resultLine.put( "course", "" );
					mResultsList.add( resultLine );
				}
			}
		}			
		else
		{
			makeCourseResults( "" ); //Empty string means select all courses.
		}
	}
	
	/**
	 * Makes the list of results for a single course.
	 * @param sCourseName
	 */
	private void makeCourseResults( String sCourseName )
	{		
		HashMap<String, String> resultLine;
		Competitor competitor;
		ControlCard controlCard;
		int nResultNumber = 0;
		int nCompetitorPosition = 1;
		String[] sSortSql = getResources().getStringArray( R.array.sortSql );

		Cursor cursor = this.getContentResolver().query( 
				DibHqLocalStore.CONTENT_URI, null, sCourseName, null, sSortSql[mnSortOrder] );
				
		if ( cursor.moveToFirst() ) 
		{
			do 
			{
				resultLine = new HashMap<String, String>();
				controlCard = new ControlCard( cursor );
				competitor = new Competitor( cursor );
							
				//Course Name
				resultLine.put( "course", controlCard.getName() );
				
				//Competitor Data
				resultLine.put( "forename",			competitor.getForename() );
				resultLine.put( "surname",			competitor.getSurname() );
				resultLine.put( "competitor", 		competitor.getFullName() );
				resultLine.put( "club", 			competitor.getClub() );
				resultLine.put( "classification",	competitor.getClassification() );
				resultLine.put( "_ID", 				competitor.getIdentifier() );
				
				//Competitor's Time or Status for the course.
				if ( controlCard.getResultStatus( this ).equals( getString( R.string.ok ) ) )
				{
					resultLine.put( "time", mSdf_UTC_HH_mm_ss.format( controlCard.getCourseTime() ) );
				}
				else
				{
					resultLine.put( "time", controlCard.getResultStatus( this ) );
				}				
				
				//If the sort order is "Course" overwrite the course field with the placings.
				if ( mnSortOrder == mnCOURSE_ORDER )
				{
					nResultNumber++;
					if ( controlCard.getResultStatus( this ).equals( getString( R.string.ok ) ) ) 
					//Status is OK. Overwrite the course field with the competitor's position.
					{ 
						//Update the result on the previous line if this result is equal to it.
						if ( mSdf_UTC_HH_mm_ss.format( controlCard.getCourseTime() ).equals( mResultsList.get( mResultsList.size()-1 ).get( "time" ) ) )
							{ 	//Put this and the previous result as "=".
								resultLine.put( "course", nCompetitorPosition + "=" ); 
								mResultsList.get( mResultsList.size()-1 ).put( "course", nCompetitorPosition + "=" ); 
							}
						else
							{
								resultLine.put( "course", Integer.toString( nResultNumber ) ); 
								nCompetitorPosition = nResultNumber;
							}
					}
					else //Status is not OK. Overwrite the course field with a blank.
					{
						resultLine.put( "course", "" );
					}
				}
				mResultsList.add( resultLine );
			} 
			while ( cursor.moveToNext() );	
		}
		
		cursor.close();
	}
	
	//--------------Functions to download a result by QR code.
	// onQrScanRequest( View view )
	// onActivityResult( int requestCode, int resultCode, Intent intent )
	
	/**
	 * Calls up the zxing or toasts if zxing isn't available.
	 */
	public void onQrScanRequest()	
	{			
		Intent intent = new Intent( "com.google.zxing.client.android.SCAN" );
		List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, PackageManager.MATCH_DEFAULT_ONLY );    
		if( list.size() > 0 ) // zxing is available
		{
			intent.setPackage( "com.google.zxing.client.android" );
			intent.putExtra( "MODE", "SCAN_MODE" );
			startActivityForResult( intent, mQR_SCAN_ACTIVITY );
		}
		else // zxing is not available
		{
			makeToast( getString( R.string.noZxing ) );
		}
	}
	
	/**
	 * Reacts to the result of a QR scan. If the scan was successful sends the result to the result receiver. 
	 * Makes a toast if the scan was not of a dib QR code or nothing was scanned.
	 */
	public void onActivityResult( int requestCode, int resultCode, Intent intent )
	{		
		switch( requestCode )
		{
			case( mQR_SCAN_ACTIVITY ):
				if ( resultCode == RESULT_OK )
				{
					String sQrText = intent.getStringExtra( "SCAN_RESULT" );
					String[] sQrArray = sQrText.split( "," );
					if ( sQrArray[0].equals( Msg.CODE_WORD ) ) 
					{
						//Send a broadcast to the result receiver.
						Intent broadcastIntent = new Intent();
						broadcastIntent.setAction( DibHqMain.msDIB_STORE_RESULT_ACTION );
						broadcastIntent.putExtra( "result", sQrArray );
						sendBroadcast( broadcastIntent );
					}
					else
					{
						Toast.makeText( this, getString( R.string.notDibResult ), Toast.LENGTH_LONG ).show();	
					}
				}
				else
				{
					Toast.makeText( this, getString( R.string.notScanned ), Toast.LENGTH_LONG ).show();
				}
				break;
		}
	}	
	
	//-----------------Menu functions------------------
	// onCreateOptionsMenu( Menu menu )
	// onOptionsItemSelected( MenuItem item )
	// shareSimpleResults()
	// deleteResults()
	// about()
	
	/**
	 * Inflates the onCreateOptionsMenu, nothing more.
	 */
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.dib_hq, menu );
		return true;
	}
	
	/**
	 * Switch statement to take action on the selected menu item.
	 */
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{		
		switch ( item.getItemId() )
		{
		case R.id.action_scan:
			onQrScanRequest();
			return true;
		case R.id.action_export:
			exportAll();
			return true;
		case R.id.action_share:
			exportAll();
			shareResults();
			return true;
		case R.id.action_delete:
			deleteResults();
			return true;
		case R.id.action_about:
			about();
			return true;
		}
		return true;
	}
	
	/**
	 * Calls methods for each of export simple, csv and xml results.
	 */
	private void exportAll() {
		ExportSplitsBrowserCsv splitsBrowser = new ExportSplitsBrowserCsv( this );
		ExportIofXml203 iofXml203 = new ExportIofXml203( this );
		exportSimpleResults();
	}
	
	private void exportSimpleResults()
	{ 	
		// File declarations.
		File file = null;
		StorageLocation storageLocation;		
		FileOutputStream fileOutputStream = null;		
		OutputStreamWriter outputStreamWriter = null;
		
		// See if you can create a file.
		storageLocation = new StorageLocation( this, getString( R.string.simpleResultsFilename ) );
		file = storageLocation.getFile();
		
		// Output columns declarations.		
		final String sCOLUMN_NAMES[] = { "course", "competitor", "club", "classification", "time" };
		final int iCOLUMN_WIDTHS[] = { 12, 28, 20, 7, 8 };
		final String sOUTPUT_FORMAT = "%-12s %-28s %-20s %-7s %-8s";

		final String resultData[] = { "","","","","" };
		HashMap<String, String> resultMap = new HashMap<String, String>();				
		String sOutputString = "";		

		for ( int nResultNumber=0; nResultNumber < mResultsList.size(); nResultNumber++ )
		{
			resultMap = mResultsList.get( nResultNumber );
			for ( int nResultColumn=0; nResultColumn < sCOLUMN_NAMES.length; nResultColumn++ )
			{
				if ( resultMap.get( sCOLUMN_NAMES[nResultColumn] ) != null )
					{ resultData[nResultColumn] = resultMap.get( sCOLUMN_NAMES[nResultColumn] ).toString(); }
				else
					{ resultData[nResultColumn] = ""; }
			}
			sOutputString += String.format( sOUTPUT_FORMAT, 						
					resultData[0].substring(0, Math.min( resultData[0].length(), iCOLUMN_WIDTHS[0] ) ), 
					resultData[1].substring(0, Math.min( resultData[1].length(), iCOLUMN_WIDTHS[1] ) ),
					resultData[2].substring(0, Math.min( resultData[2].length(), iCOLUMN_WIDTHS[2] ) ),
					resultData[3].substring(0, Math.min( resultData[3].length(), iCOLUMN_WIDTHS[3] ) ),
					resultData[4].substring(0, Math.min( resultData[4].length(), iCOLUMN_WIDTHS[4] ) )
					) + "\r\n";
		}
		
		if ( file != null )
		{
			file.delete();
			
			try
			{	//TODO 8K bytes buffer size on OutputStreamWriter...may not be enough? Unlikely.
				fileOutputStream = new FileOutputStream( file );
				outputStreamWriter = new OutputStreamWriter( fileOutputStream );
				outputStreamWriter.write( sOutputString );
				outputStreamWriter.flush();
				if ( outputStreamWriter != null ) outputStreamWriter.close();
				if ( fileOutputStream != null ) fileOutputStream.close();
			}
			catch ( Exception e ) {
				String sMessage = getString( R.string.unable_to_save ) + getString( R.string.simpleResultsFilename );
				Toast.makeText( this, sMessage, Toast.LENGTH_LONG).show();
			} //TODO			
		}
	}
	
	/**
	 * Launches a dialog to confirm if results should be deleted.
	 */
	private void deleteResults()
	{
		AlertDialog.Builder adbDelete = new AlertDialog.Builder( this );
		
		//Title & Icon & Message.
		adbDelete.setTitle( getString( R.string.delete ) );
		adbDelete.setIcon( R.drawable.ic_launcher );
//		adbDelete.setIcon( android.R.drawable.ic_dialog_alert );
		adbDelete.setMessage( getString( R.string.deleteAllResults ) );
		
		//Buttons.
		adbDelete.setCancelable( true );
		adbDelete.setNegativeButton( getString( R.string.no ), 
			new DialogInterface.OnClickListener() 
			{	
				@Override
				public void onClick( DialogInterface dialog, int which )
				{
					dialog.cancel();
				}
			});
		adbDelete.setPositiveButton( getString( R.string.yes ), 
			new DialogInterface.OnClickListener() 
			{	
				@Override
				public void onClick( DialogInterface dialog, int which )
				{
					getBaseContext().getContentResolver().delete( DibHqLocalStore.CONTENT_URI, null, null );
					refreshResultsDisplay();
				}
			});
		adbDelete.show();
	}	
	
	private void about()
	{
		AlertDialog.Builder adbAbout = new AlertDialog.Builder(this);
		TextView tvWebsiteLink = new TextView(this);
		PackageManager pm = getPackageManager();
		PackageInfo pi = null;
		String sTitle = "";
	
		//Title
		sTitle = getString( R.string.app_name ) + " " + getString( R.string.version );
		try { 
			pi = pm.getPackageInfo( this.getPackageName(), 0  );
			sTitle += pi.versionName; 
		}			
		catch ( NameNotFoundException e ) {}
		adbAbout.setTitle( sTitle );
		
		//Icon & Message
		adbAbout.setIcon( R.drawable.ic_launcher );	
		adbAbout.setMessage( getString( R.string.copyright ) );
		
		//Website link.
		tvWebsiteLink.setMovementMethod( LinkMovementMethod.getInstance() );
		tvWebsiteLink.setText( R.string.website );
		tvWebsiteLink.setGravity( Gravity.CENTER | Gravity.CENTER );
		adbAbout.setView( tvWebsiteLink );
		
		//Buttons.
		adbAbout.setCancelable( true );
		adbAbout.setPositiveButton( getString( R.string.ok ), 
			new DialogInterface.OnClickListener() {
				public void onClick( DialogInterface dialog, int whichButton ) 
					{ dialog.cancel(); }
			});
		
		adbAbout.show();
	}
	
	//-------------Utility functions--------------------
	// makeToast( String sToastText )
	// initialiseSortMatrix()
	// applyPreferences()
	
	private void makeToast( String sToastText )
	{
		Toast.makeText( this, sToastText, Toast.LENGTH_LONG ).show();		
	}

	/**
	 * Shares all result files - the menu action will have firstly created the result files.
	 */
	private void shareResults()
	{		
		File sdCard = Environment.getExternalStorageDirectory();
		String path = sdCard.getAbsoluteFile() + "/dibHq/";
		
		ArrayList<Uri> uris = new ArrayList<Uri>();
		String[] filePaths = new String[] { 
				path + getString( R.string.simpleResultsFilename ),
				path + getString( R.string.winsplitsFilename ),
				path + getString( R.string.splitsbrowserFilename ) };
		for ( String file: filePaths )
		{
			File fileIn = new File( file );
			Uri u = Uri.fromFile( fileIn );
			uris.add( u );
		}
		
		Intent sharingIntent = new Intent();
		sharingIntent.setAction( Intent.ACTION_SEND_MULTIPLE );
		sharingIntent.setType( "text/Message" );
		sharingIntent.putExtra( Intent.EXTRA_SUBJECT, getString( R.string.dibResults ) );
		sharingIntent.putParcelableArrayListExtra( Intent.EXTRA_STREAM, uris );
		startActivity( Intent.createChooser( sharingIntent, getString( R.string.shareResultsVia ) ) );			
	}
	
}