package com.joji.hwasel;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;


import com.stericson.RootTools.*;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
	//List<String> hwuiallowed;
	//String delimiter=":";
	ProgressDialog progressDialog;
	boolean selectall=false;
	String HWUI_ALLOW="/data/local/hwui.allow/";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog=new ProgressDialog(this);
        
        AsyncLoad al=new AsyncLoad();
        al.execute();
    }
    
	//Refresh the selections
    private void refreshList() 
    {
    	for (int j = 0; j < packages.size(); j++)
        {
        	ApplicationInfo packageInfo=packages.get(j);
        	if(isWhitelist(packageInfo.packageName))
        		listView.setItemChecked(j, true);
        	else
        		listView.setItemChecked(j, false);
        }
	}
    private boolean isWhitelist(String packName)
    {
    	return new File(HWUI_ALLOW+packName).exists();
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
		        	RootTools.sendShell("chmod 0777 /data/local/", 0);
		        	RootTools.sendShell("chmod 0777 /data/local/hwui.allow/", 0);
		        	
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
			//delimiter=":";
		}
		else if(buildhost.equals("quarx-VirtualBox"))
		{
			//Quarx build
			Toast.makeText( getApplicationContext(), "Quarx Build Detected", Toast.LENGTH_LONG).show();
			//delimiter=".";
		}
		else if(buildhost.equals("fuzz"))
		{
			//Fuzz build
			Toast.makeText( getApplicationContext(), "Fuzz Build Detected", Toast.LENGTH_LONG).show();
			//delimiter=":";
		}
		else
		{
			Toast.makeText( getApplicationContext(), "Unable to detect build:"+buildhost, Toast.LENGTH_LONG).show();
			}
	}
			
	
	
	//Generates whitelist from selected items
	
	//Saves the proptext into local.prop with new whitelist
	private void save() throws IOException, RootToolsException, TimeoutException 
	{
		AsyncApply aa=new AsyncApply();
        aa.execute();
		
	}
	
	//Asynctask to apply changes without restarting
	private class AsyncApply extends AsyncTask<Void, Void, Void>
	{
	
		@Override
		protected Void doInBackground(Void... params) //Loads prop into aBuffer and return list of apps installed
		{
			        
			List<String> res=null;
			boolean result=RootTools.remount("/data/", "rw");
			try {
				if(result)
				{
					res=RootTools.sendShell("cd "+HWUI_ALLOW+"; ls", 0);
					if(new File(HWUI_ALLOW).exists());
					{
						//delete it
						RootTools.sendShell("rm -rf "+HWUI_ALLOW, 0);
					}
					
					RootTools.sendShell("mkdir "+HWUI_ALLOW, 0);
	
					SparseBooleanArray sel=	listView.getCheckedItemPositions();
					
					for(int i=0;i<packages.size();i++)
					{
						if(sel.get(i))
						{
							
								RootTools.sendShell("touch "+HWUI_ALLOW+packages.get(i).packageName, 0);
							
						}
					}
					
					 
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Unable to mount /data", Toast.LENGTH_SHORT).show();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RootToolsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         boolean now;
	         boolean prev;
	         for(ApplicationInfo appinfo: packages)
	         {
	        	 now=isWhitelist(appinfo.packageName);
	        	 prev=res.contains(appinfo.packageName);
	        	 if(((!now&&prev)||(now&&!prev)))
	        	 {
	        		 Log.d("Close",appinfo.packageName);
	        		try {
	     	        	
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
		//String wui=getWhitelistString();
		for (int j = 0; j < packages.size(); j++)
		{
			ApplicationInfo packageInfo=packages.get(j);
			if(isWhitelist(packageInfo.packageName))
			{
				if(output!="")
					output+=","+packageInfo.packageName;
			    else
			    	output+=packageInfo.packageName;
			 }
		}
		
		File file = new File("/sdcard/hwa_backup");
	 
		 try
		 {
			 FileWriter filewriter = new FileWriter(file);
			 BufferedWriter out = new BufferedWriter(filewriter);
			 out.write(output);
	         Toast.makeText(getApplicationContext(), "Succesfully Backed Up", Toast.LENGTH_SHORT).show();
	         out.close();
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
			
			for (int j = 0; j < packages.size(); j++)
	        {
	        	ApplicationInfo packageInfo=packages.get(j);
	        	if(restoreText.contains(","+packageInfo.packageName)||restoreText.contains(packageInfo.packageName+","))
	        		listView.setItemChecked(j, true);
	        	else
	        		listView.setItemChecked(j, false);
	        }
			
				save();
			
			Toast.makeText(getApplicationContext(), "Backup Restored", Toast.LENGTH_SHORT).show();
		   
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "No backups found", Toast.LENGTH_SHORT).show();
		       
		}catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "Cannot access SDCARD", Toast.LENGTH_SHORT).show();
			       
		} catch (RootToolsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			try {
				save();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RootToolsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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