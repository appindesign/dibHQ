package com.appindesign.dibhq;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

public class ListOfCourses {

	private List<String> sList;
	private int numberOfCourses;

	ListOfCourses( Context context ) {

		sList = new ArrayList<String>();
		numberOfCourses = 0;
		
		final String[] sPROJECTION = { "DISTINCT COL_NAME" };
		final String sSORT_ORDER = "COL_NAME ASC";

		Cursor cursor = context.getContentResolver().query(
			DibHqLocalStore.CONTENT_URI, 
			sPROJECTION, 
			null, null,
			sSORT_ORDER);
		
		if ( cursor.moveToFirst() ) {
			do {
				sList.add( cursor.getString(0) );
			} while (cursor.moveToNext());
		}
		
		cursor.close();

		numberOfCourses = sList.size();
	}

	public String getName( int nCourseNumber ) {
		return sList.get( nCourseNumber );
	}
	
	public int getNumberOfCourses() {
		return numberOfCourses;
	}
}
