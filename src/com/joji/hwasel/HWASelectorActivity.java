package com.joji.hwasel;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;


import com.stericson.RootTools.*;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HWASelectorActivity extends ListActivity {
	
	
	String propText;
	ListView listView;
	List<ApplicationInfo> packages;
	String delimiter=":";
	ProgressDialog progressDialog;
	boolean selectall=false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog=new ProgressDialog(this);
        propText="";
        AsyncLoad al=new AsyncLoad();
        al.execute();
    }
    
	//Refresh the selections
    private void refreshList() 
    {
    	String wui=getWhitelistString();
        for (int j = 0; j < packages.size(); j++)
        {
        	ApplicationInfo packageInfo=packages.get(j);
        	if(wui.contains(packageInfo.packageName)&&!packageInfo.packageName.equals("android"))
    		listView.setItemChecked(j, true);
    	else
    		listView.setItemChecked(j, false);
      }
	}
	
	
	//Get the whitelist string from loaded proptext
	private String getWhitelistString()
	{
		String wui="";
		int startw=propText.indexOf("hwui.whitelist");
		if(startw>=0)
		{
			//Common for both builds
			int endw=propText.indexOf('\n',startw);
			
			Log.d("-",startw+" - "+endw);
			wui=propText.substring(startw+15, endw);
			//Log.d("Res",wui);
		}
		return wui;
			
	}
	
	//Get the blacklist string from loaded proptext
	private String getBlacklistString()
	{
		String bui="";
	int startb=propText.indexOf("hwui.blacklist");
	if(startb>0)
	{
		//Common for both builds
		int endb=propText.indexOf('\n',startb);
		bui=propText.substring(startb+15, endb);
		Log.d("Res",bui);
		}
		return bui;
		
	}
	
	//AsyncTask for loading local.prop
	private class AsyncLoad extends AsyncTask<Void, Void, String[]>
	{
	
		@Override
		protected String [] doInBackground(Void... params) //Loads prop into aBuffer and return list of apps installed
		{
			String strArray[];
			if (RootTools.isAccessGiven()) {
		           
		        RootTools.remount("/data/", "rw");
		        try {
		        	//RootTools.sendShell("chmod 0777 /system/build.prop", 0);
		        	RootTools.sendShell("chmod 0777 /data/", 0);
		        	RootTools.sendShell("chmod 0777 /data/local.prop", 0);
		        	
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (RootToolsException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (TimeoutException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        
		        
		    	try 
		    	{        		
		    		//loadBuildprop();
		    		
		    		//Loading local Prop
		    		loadlocalprop();
		    		
		    		//Get List of Installed Apps,sort and put into array
		    		final PackageManager pm = getPackageManager();
			      
		            packages= pm.getInstalledApplications(PackageManager.GET_META_DATA);
		            Collections.sort(packages, new AppSort(pm));
		            
		            strArray=new String[packages.size()];
		            for (int j = 0; j < packages.size(); j++) 
		            {
		            	ApplicationInfo packageInfo=packages.get(j);
		            	strArray[j]=(String) pm.getApplicationLabel(packageInfo);
		            	
		            }
		            return strArray;
		    	} catch (IOException e) {
		    	    // something went wrong, deal with it hereandroid 
		    	}
			}
			
			return null;
		}
	
		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			progressDialog.setMessage("Loading...");
		    progressDialog.setIndeterminate(true);
			progressDialog.show();
			
		}
		@Override
		protected void onPostExecute(String strArray[])
		{
			super.onPostExecute(strArray);
			progressDialog.hide();
			
			getBuild();
			
			
			progressDialog.hide();
			setListAdapter(new ArrayAdapter<String>(HWASelectorActivity.this,
		            android.R.layout.simple_list_item_multiple_choice, strArray));
		   listView = getListView();
		    listView.setItemsCanFocus(false);
		    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		   // listView.setFastScrollEnabled(true);
		    refreshList();
		}
	
	
		
		
	}
	
	//Displays Build and sets delimiter
	private void getBuild() 
	{
		String buildhost="";
		 try {
	        	//RootTools.sendShell("chmod 0777 /system/build.prop", 0);
	        	List<String> out= RootTools.sendShell("getprop ro.build.host", 0);
	        	buildhost=out.get(0);
	        	
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (RootToolsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TimeoutException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(buildhost.equals("epsytour"))
		{
			//Epsylon Build
			Toast.makeText( getApplicationContext(), "Epsylon Build Detected", Toast.LENGTH_LONG).show();
			delimiter=":";
		}
		else if(buildhost.equals("quarx-VirtualBox"))
		{
			//Quarx build
			Toast.makeText( getApplicationContext(), "Quarx Build Detected", Toast.LENGTH_LONG).show();
			delimiter=".";
		}
		else if(buildhost.equals("fuzz"))
		{
			//Fuzz build
			Toast.makeText( getApplicationContext(), "Fuzz Build Detected", Toast.LENGTH_LONG).show();
			delimiter=":";
		}
		else
		{
			Toast.makeText( getApplicationContext(), "Unable to detect build:"+buildhost, Toast.LENGTH_LONG).show();
			}
	}
			
	//Loads the file
	private void loadlocalprop() throws IOException,FileNotFoundException
	{
		File myFile = new File("/data/local.prop");
		if(myFile.exists())
		{
			FileInputStream fIn = new FileInputStream(myFile);
			BufferedReader myReader = new BufferedReader(
					new InputStreamReader(fIn));
			String aDataRow = "";
			propText = "";
			while ((aDataRow = myReader.readLine()) != null) 
			{
				propText += aDataRow + "\n";
				
			}
			myReader.close();
			//aBuffer=aBuffer.replace("#hwui.whitelist", "hwui.whitelist");
			propText=propText.replace("#hwui.blacklist", "hwui.blacklist");
			propText=propText.replace("hwui.blacklist", "#hwui.blacklist");
		}
		else
		{
			propText="";
		}
		
		
	}
	
	//Generates whitelist from selected items
	private String generateWhitelistFromSelected()
	{
		//Get SeletectedItems
		SparseBooleanArray sel=	listView.getCheckedItemPositions();
		String whitelist="";
		
		for(int i=0;i<packages.size();i++)
		{
			if(sel.get(i))
			{
				if(whitelist=="")
					whitelist+=packages.get(i).packageName;
				else
					whitelist+=delimiter+packages.get(i).packageName;
			}
		}
		
		if(whitelist.equals(""))
			whitelist="-none";
		
		
		return whitelist;
	}
	
	//Saves the proptext into local.prop with new whitelist
	private void save() 
	{
		boolean result=RootTools.remount("/data/", "rw");
		if(result)
		{
			String whitelist=generateWhitelistFromSelected();
			String prevwhitelist=getWhitelistString();
			//If already whitelist is not present
			
			if(propText.equals(""))
				propText="hwui.whitelist="+whitelist+"\n";
			else
				propText=propText.replace("hwui.whitelist="+prevwhitelist+"\n", "hwui.whitelist="+whitelist+"\n");
			
			
			//Write to file
			
			File file = new File("/data/local.prop");
			 
			 try 
			 {
				 file.createNewFile();
				 if (file.canWrite())
			     {
			    	 FileWriter filewriter = new FileWriter(file);
			         BufferedWriter out = new BufferedWriter(filewriter);
			         
		             out.write(propText);
		            // Toast.makeText(getApplicationContext(), "Succesfully Changed Properties", Toast.LENGTH_SHORT).show();
		             out.close();
			    	 
		             AsyncApply aa=new AsyncApply();
		             aa.execute(whitelist,prevwhitelist);
		         }
			     else
			     {
			    	 Toast.makeText(getApplicationContext(), "Cannot Write to local.prop", Toast.LENGTH_SHORT).show();
			             
		         }
		     
			 } catch (IOException e) {
			    Log.e("TAG", "Could not write file " + e.getMessage());
			 }
	
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Unable to mount /data", Toast.LENGTH_SHORT).show();
		}
	}
	
	//Asynctask to apply changes without restarting
	private class AsyncApply extends AsyncTask<String, Void, Void>
	{
	
		@Override
		protected Void doInBackground(String... params) //Loads prop into aBuffer and return list of apps installed
		{
			String whitelist=params[0];
			String prevwhitelist=params[1];
			try {
 	        	//RootTools.sendShell("chmod 0777 /system/build.prop", 0);
 	        	List<String> a= RootTools.sendShell("setprop hwui.whitelist "+whitelist, 0);
 	        	
 	        	
 			} catch (IOException e1) {
 				// TODO Auto-generated catch block
 				e1.printStackTrace();
 			} catch (RootToolsException e1) {
 				// TODO Auto-generated catch block
 				e1.printStackTrace();
 			} catch (TimeoutException e1) {
 				// TODO Auto-generated catch block
 				e1.printStackTrace();
 			}
         
         boolean now;
         boolean prev;
         for(ApplicationInfo appinfo: packages)
         {
        	 now=whitelist.contains(appinfo.packageName);
        	 prev=prevwhitelist.contains(appinfo.packageName);
        	 if(((!now&&prev)||(now&&!prev))&&!appinfo.packageName.equals("android"))
        	 {
        		 Log.d("Close",appinfo.packageName);
        		 try {
     	        	//RootTools.sendShell("chmod 0777 /system/build.prop", 0);
     	        	List<String> a= RootTools.sendShell("killall "+appinfo.packageName, 0);
     	        	
     	        	
     			} catch (IOException e1) {
     				// TODO Auto-generated catch block
     				e1.printStackTrace();
     			} catch (RootToolsException e1) {
     				// TODO Auto-generated catch block
     				e1.printStackTrace();
     			} catch (TimeoutException e1) {
     				// TODO Auto-generated catch block
     				e1.printStackTrace();
     			}
        		 
        	 }
        	 
         }
			return null;
		}
	
		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			progressDialog.setMessage("Applying Settings...");
		    progressDialog.setIndeterminate(true);
			progressDialog.show();
			
		}
		@Override
		protected void onPostExecute(Void a)
		{
			super.onPostExecute(a);
			progressDialog.hide();
			Toast.makeText(getApplicationContext(), "Changes have been applied", Toast.LENGTH_SHORT).show();
	          
		}
	
	
		
		
	}
	
	//Create a backup of whitelist
	private void saveBackup() 
	{
		String output="";
		String wui=getWhitelistString();
		for (int j = 0; j < packages.size(); j++)
		{
			ApplicationInfo packageInfo=packages.get(j);
			if(wui.contains(packageInfo.packageName)&&!packageInfo.packageName.equals("android"))
				
			if(output!="")
				output+=","+packageInfo.packageName;
		    else
		    	output+=packageInfo.packageName;
		 }
		File file = new File("/sdcard/hwa_backup");
	 
		 try
		 {
			 FileWriter filewriter = new FileWriter(file);
			 BufferedWriter out = new BufferedWriter(filewriter);
			 out.write(output);
	         Toast.makeText(getApplicationContext(), "Succesfully Backed Up", Toast.LENGTH_SHORT).show();
		 } 
		 catch (IOException e) {
			 	Log.e("TAG", "Could not write file " + e.getMessage());
			 	Toast.makeText(getApplicationContext(), "Cannot Access SDCARD", Toast.LENGTH_SHORT).show();
		 }
	}
	
	//Restore whitelist backup
	private void restoreBackup() 
	{
		try {
	    	File myFile = new File("/sdcard/hwa_backup");
	    	FileInputStream fIn;
		
			fIn = new FileInputStream(myFile);
		
			BufferedReader myReader = new BufferedReader(
				new InputStreamReader(fIn));
			String aDataRow = "";
			String restoreText="";
			while ((aDataRow = myReader.readLine()) != null) 
			{
				restoreText += aDataRow + "\n";
				
			}
		
		
			myReader.close();
			restoreText.replace(",",delimiter);
			
			if(propText.equals(""))
				propText="hwui.whitelist="+restoreText+"\n";
			else
				propText=propText.replace("hwui.whitelist="+getWhitelistString()+"\n", "hwui.whitelist="+restoreText+"\n");
			
			refreshList();
			Toast.makeText(getApplicationContext(), "Backup Restored", Toast.LENGTH_SHORT).show();
		    save();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "No backups found", Toast.LENGTH_SHORT).show();
		       
		}catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "Cannot access SDCARD", Toast.LENGTH_SHORT).show();
			       
		}
	}
	
	
	//Comparator for sorting app names
	private class AppSort implements Comparator<ApplicationInfo>
	{
		PackageManager pm;
		
		public AppSort(PackageManager pm)
		{
			super();
			this.pm=pm;
		}
	
		@Override
		public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
		// TODO Auto-generated method stub
			
			return this.pm.getApplicationLabel(lhs).toString().compareTo(this.pm.getApplicationLabel(rhs).toString());
		}
    	
    }
    
	   
	    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
    	switch (item.getItemId()) {
		    case R.id.menu_save:
		        save();
		        return true;
		        
		    case R.id.menu_selall:
		    	selectall=!selectall;
			    for ( int i=0; i< getListAdapter().getCount(); i++ ) {
			            listView.setItemChecked(i, selectall);
			            
			    }
		        return true;   
	        
	    case R.id.menu_backup:
	    	if(new File("/sdcard/hwa_backup").exists())
	    	{
	    		AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
	        	abuilder.setMessage("Do you want to overwrite backup?");
	        	       abuilder.setCancelable(false);
	        	       abuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	        	   saveBackup();
	        	           }
	        	       });
	        	       abuilder.setNegativeButton("Cancel backup", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	                //do things
	        	           }
	        	       });
	        	AlertDialog alert = abuilder.create();
	        	alert.show();
	    		
	    	}
	    	else
	    	{
	    		saveBackup();
	    	}
	    		
	    		
	    		
	    	
	        
	        return true;
	        
	    case R.id.menu_restore:
	        restoreBackup();
	        return true;
	            
	    case R.id.menu_about:
	    	AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
	    	abuilder.setMessage(R.string.app_about);
	    	
	    	abuilder.setCancelable(false);
	    	       abuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                //do things
	            	           }
	            	       });
	            	AlertDialog alert = abuilder.create();
	            	alert.show();
	                		return true;
	            default:
	                return super.onOptionsItemSelected(item);
	        }
	    }
	    
	    
	 
	
}