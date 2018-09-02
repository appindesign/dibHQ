package com.appindesign.dibhq;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.appindesign.diblibrary.Competitor;
import com.appindesign.diblibrary.ControlCard;
import com.appindesign.diblibrary.Msg;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

public class ExportIofXml203 
{
	ExportIofXml203( Context context )
	{
		ListOfCourses listOfCourses;
		Competitor competitor;
		ControlCard controlCard;	

		String sResultStatus = "";

		String sSqlWhere = "";		
		Cursor cursor;		
		
		final SimpleDateFormat sdf_Local_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss", Locale.getDefault() );
		final SimpleDateFormat sdf_UTC_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss" );
		sdf_UTC_HH_mm_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		
		//Code
		listOfCourses = new ListOfCourses( context );		
		
		try 
		{			
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();	
			
			Element resultList = document.createElement( "ResultList" );
			resultList.setAttribute( "status", "complete" );			
			document.appendChild( resultList );
			
			Element iofVersion = document.createElement( "IOFVersion" );
			iofVersion.setAttribute( "version", "2.0.3" );
			resultList.appendChild( iofVersion );
			
			Element classShortName;
			for ( int nCourseNumber=0; nCourseNumber < listOfCourses.getNumberOfCourses(); nCourseNumber++ )
			{
				Element classResult = document.createElement( "ClassResult" );
				
				classShortName = document.createElement( "ClassShortName" );
				classResult.appendChild( classShortName );
				classShortName.appendChild( document.createTextNode( listOfCourses.getName(nCourseNumber) ) );
				
				//For each course find each result.
				sSqlWhere = "COL_NAME = \'" + listOfCourses.getName(nCourseNumber) + "\'";
				cursor = context.getContentResolver().query( DibHqLocalStore.CONTENT_URI, null, sSqlWhere, null, null );
				
				if ( cursor.moveToFirst() )
				{
					do
					{
						competitor = new Competitor( cursor );
						controlCard = new ControlCard( cursor );
						
						if ( controlCard.getNumberOfControls() != 0 )
						{
							
							Element personResult = document.createElement( "PersonResult" );
							classResult.appendChild( personResult );
							
							Element person = document.createElement( "Person" );
							personResult.appendChild( person );
							
							Element personName = document.createElement( "PersonName" );
							person.appendChild( personName );
							
							Element family = document.createElement( "Family" );
							personName.appendChild( family );
							family.appendChild( document.createTextNode( competitor.getSurname() ) );
							
							Element given = document.createElement( "Given" );
							personName.appendChild( given );
							given.appendChild( document.createTextNode( competitor.getForename() ) );
							
							Element club = document.createElement( "Club" );
							personResult.appendChild( club );							
								Element name = document.createElement( "Name" );
								club.appendChild( name );
								name.appendChild( document.createTextNode( competitor.getClub() ) );	
							
							Element resultElement = document.createElement( "Result" );
							personResult.appendChild( resultElement );
							
							Element identifier = document.createElement( "CCardId" );
							resultElement.appendChild( identifier );
							identifier.appendChild( document.createTextNode( competitor.getIdentifier() ) );							
							
							Element clockElement = document.createElement( "Clock" );
							if ( controlCard.getStartTimestamp() != Msg.INVALID_TIME )
							{
								Element startTime = document.createElement( "StartTime" );
								resultElement.appendChild( startTime );
									clockElement.setAttribute( "clockFormat", "HH:MM:SS" );
									clockElement.appendChild( document.createTextNode( sdf_Local_HH_mm_ss.format( controlCard.getStartTimestamp() ) ) );
								startTime.appendChild( clockElement );				
							}
							
							if ( controlCard.getFinishTimestamp() != Msg.INVALID_TIME )
							{							
								Element finishTime = document.createElement( "FinishTime" );
								resultElement.appendChild( finishTime );							
									clockElement = document.createElement( "Clock" );
									clockElement.setAttribute( "clockFormat", "HH:MM:SS" );
									clockElement.appendChild( document.createTextNode( sdf_Local_HH_mm_ss.format( controlCard.getFinishTimestamp() ) ) );	
								finishTime.appendChild( clockElement );									
							}				

							Element time = document.createElement( "Time" );
							if ( controlCard.getResultStatus( context ).equalsIgnoreCase( "OK" ) )
							{
								time.setAttribute( "timeFormat", "HH:MM:SS" );
								time.appendChild( document.createTextNode( sdf_UTC_HH_mm_ss.format( controlCard.getCourseTime() ) ) );
								resultElement.appendChild( time );
							}
							
							Element competitorStatus = document.createElement( "CompetitorStatus" );				
							sResultStatus = controlCard.getResultStatus( context );
							if ( sResultStatus.equals( context.getString( R.string.mp ) ) ) 
								sResultStatus = context.getString( R.string.xmlMp );
							if ( sResultStatus.equals( context.getString( R.string.dns ) ) ) 
								sResultStatus = context.getString( R.string.xmlDns );
							if ( sResultStatus.equals( context.getString( R.string.dnf ) ) ) 
								sResultStatus = context.getString( R.string.xmlDnf );
							competitorStatus.setAttribute( "value", sResultStatus );
							resultElement.appendChild( competitorStatus );
							
							Element courseLength = document.createElement( "CourseLength" );
							resultElement.appendChild( courseLength );
							courseLength.appendChild( document.createTextNode( controlCard.getLength() ) );

							//Run through each control on the course.
							for ( int i=1; i <= controlCard.getNumberOfControls(); i++ )
							{	
								//Sequence
								Element splitTimestamp = document.createElement( "SplitTime" );
								splitTimestamp.setAttribute( "sequence", Integer.toString(i) );
								resultElement.appendChild( splitTimestamp );
								
								//Code
								Element code = document.createElement( "ControlCode" );
								splitTimestamp.appendChild( code );
								code.appendChild( document.createTextNode( controlCard.getCode(i) ) );
								
								//Split Timestamp
								time = document.createElement( "Time" );
								time.setAttribute( "timeFormat", "HH:MM:SS" );
								if ( controlCard.getTimestamp(i) == Msg.INVALID_TIME )
								{
									time.appendChild( document.createTextNode( context.getString(R.string.xmlMp) ) );							
									splitTimestamp.appendChild( time );
								}
								else
								{
									if ( controlCard.getElapsedTime(i) != Msg.INVALID_TIME ) {
										time.appendChild( document.createTextNode( sdf_UTC_HH_mm_ss.format( controlCard.getElapsedTime(i) ) ) );							
										splitTimestamp.appendChild( time );	
									}								
								};
							}
						}
					}
					while ( cursor.moveToNext() );
					cursor.close();
				}
				resultList.appendChild( classResult );
			}						
			writeDomToFile( context, document );			
		}
		catch ( ParserConfigurationException e ) {}	
	}

	private void writeDomToFile( Context context, Document document )
	{
		// File variables
		File file = null;
		StorageLocation storageLocation;
		FileOutputStream fileOutputStream = null;
		StreamResult streamResult = null;
		
		// See if you can create a file.
		storageLocation = new StorageLocation( context, context.getString( R.string.winsplitsFilename ) );
		file = storageLocation.getFile();
		
		// Transformer variables.
		DOMSource domSource = new DOMSource( document.getDocumentElement() );
		TransformerFactory transformerFactory;
		Transformer transformer = null;		
		Properties outFormat;
		
		if ( file != null )
		{
			file.delete();
			
			// Prepare transformer factory.
			try {
				transformerFactory = TransformerFactory.newInstance();
				transformer = transformerFactory.newTransformer();		
				outFormat = new Properties();
					outFormat.setProperty( OutputKeys.INDENT, "yes" );
					outFormat.setProperty( OutputKeys.METHOD, "xml" );
					outFormat.setProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
					outFormat.setProperty( OutputKeys.VERSION, "1.0" );
					outFormat.setProperty( OutputKeys.ENCODING, "UTF-8" );
				transformer.setOutputProperties( outFormat );
			}
			catch ( Exception e ) {
				String sMessage = context.getString( R.string.unable_to_create ) + context.getString( R.string.winsplitsFilename );
				Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
			} //TODO
			
			try {
				fileOutputStream = new FileOutputStream( file );
				streamResult = new StreamResult( fileOutputStream );
				transformer.transform( domSource, streamResult );
				if ( fileOutputStream != null ) fileOutputStream.close();
			}
			catch ( Exception e ) {
				String sMessage = context.getString( R.string.unable_to_save ) + context.getString( R.string.winsplitsFilename );
				Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
			}			
		}
	}
}