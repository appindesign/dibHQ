package com.appindesign.dibhq;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

/**
 * Creates a dibHq file on an SD card, toasts if there is no card.
 */
public class StorageLocation {
	
	private File file;

	StorageLocation( Context context, String filename ) 
	{
		File sdCard = null;
		file = null;

		if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) ) 
		{
			sdCard = Environment.getExternalStorageDirectory();
			file = new File( sdCard.getAbsoluteFile() + "/dibHq/" + filename );
			file.mkdirs();
//			if ( !file.isFile() ) { file = null; };
		}
		else 
		{
			Toast.makeText( context, context.getString( R.string.needMedia ), Toast.LENGTH_LONG).show();
		}
	}

	public File getFile() {
		return file;
	}
}
