/* 
 * Copyright (C) 2011 Christian Schneider
 * 
 * This file is part of Tiny File Commander.
 * 
 * Tiny File Commander is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tiny File Commander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tiny File Commander.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package tritop.android.filecommander;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FileCommander extends Activity {
    private final static int FILECREATE_DIALOG = 510;
    private final static int DIRCREATE_DIALOG = 511;
    private final static int RENAME_DIALOG = 512;
    private final static int DELETE_DIALOG = 513;
    private final static int MOVE_DIALOG = 514;
    private final static int COPY_DIALOG = 515;
    
    private String mPath=null;
	private boolean mDeleteModeActive=false;
	private boolean mMoveModeActive=false;
	private boolean mCopyModeActive=false;
	private int mLastSelectedItem=0;
	private String mFileSourcePath;
	private GridView mGridView;
	private TextView mPathLine,mCopyDialogText;
	private ProgressBar mCopyDialogProBar;
	private StorageViewAdapter mFSAdapter;
	private OnClickListener mBackButtonClicked,mHomeButtonClicked,mReloadButtonClicked,mFilePlusButtonClicked,mDirPlusButtonClicked,mMoveButtonClicked,mDeleteButtonClicked;
	private ImageView mBackButtonImg,mHomeButtonImg,mReloadButtonImg,mFilePlusButtonImg,mDirPlusButtonImg,mMoveButtonImg,mDeleteButtonImg;
	private DataSetObserver mDObserver;
	private CopyTask cp;
	private SharedPreferences mSharedPref;
	private boolean mPreferenceDeleteConfirmation;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mGridView = (GridView) this.findViewById(R.id.gridViewDir);
        mPathLine = (TextView) this.findViewById(R.id.textViewPathLine);
        mBackButtonImg = (ImageView) this.findViewById(R.id.imageViewBack);
        mHomeButtonImg = (ImageView) this.findViewById(R.id.imageViewHome);
        mReloadButtonImg = (ImageView) this.findViewById(R.id.imageViewReload);
        mFilePlusButtonImg = (ImageView) this.findViewById(R.id.imageViewFilePlus);
        mDirPlusButtonImg = (ImageView) this.findViewById(R.id.imageViewDirPlus);
        mMoveButtonImg = (ImageView) this.findViewById(R.id.imageViewMove);
        mDeleteButtonImg = (ImageView) this.findViewById(R.id.imageViewDelete);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
        mFSAdapter = new StorageViewAdapter(this);
    	mGridView.setAdapter(mFSAdapter);
    	this.registerForContextMenu(mGridView);
    	setupObserver();
    	setupButtonListeners();
    	setSDRoot();
    	setDeleteButtonState();
    	setMoveButtonState();
    }

    
    //*************************************************************************
    // Setup Option Menu 
    //*************************************************************************
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inf = this.getMenuInflater();
		inf.inflate(R.menu.optionsmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.item_quit: this.finish();break;
			case R.id.itemPreferences: showPreferenceScreen();break;
			default:break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	//*************************************************************************
    // Setup Context Menu 
    //*************************************************************************
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mLastSelectedItem=info.position;
		switch (item.getItemId()) {
			case R.id.itemRename:showDialog(RENAME_DIALOG);return true;
			case R.id.itemDelete:showDialog(DELETE_DIALOG);return true;
			case R.id.itemSend:sendSelectedFile();return true;
			case R.id.itemMove:selectMoveFile();return true;
			case R.id.itemCopy:selectCopyFile();return true;
			case R.id.itemOpen:openSelectedFile();return true;
		}
		return super.onContextItemSelected(item);
	}



	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gridviewmenu, menu);
	}

	//*************************************************************************
    // Setup Dialogs
    //*************************************************************************
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog;
		final EditText input = new EditText(this);
		switch(id){
			case FILECREATE_DIALOG: 
				 builder.setTitle(this.getResources().getString(R.string.dialog_createfile_title));
				 builder.setView(input);
				 builder.setMessage(this.getResources().getString(R.string.dialog_createfile_message))
				 .setCancelable(false)
				 .setPositiveButton(this.getResources().getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 FileCommander.this.createNewFile(input.getText().toString());
					 }
				 })
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
			case DIRCREATE_DIALOG: 
				 builder.setTitle(this.getResources().getString(R.string.dialog_createdir_title));
				 builder.setView(input);
				 builder.setMessage(this.getResources().getString(R.string.dialog_createdir_message))
				 .setCancelable(false)
				 .setPositiveButton(this.getResources().getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 FileCommander.this.createNewDir(input.getText().toString());
					 }
				 })
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
			case RENAME_DIALOG:
				 builder.setTitle(this.getResources().getString(R.string.dialog_rename_title));
				 builder.setView(input);
				 builder.setMessage(this.getResources().getString(R.string.dialog_rename_message))
				 .setCancelable(false)
				 .setPositiveButton(this.getResources().getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 FileCommander.this.renameSelectedFile(input.getText().toString());
					 }
				 })
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
			case DELETE_DIALOG:
				 builder.setTitle(this.getResources().getString(R.string.dialog_delete_title));
				 builder.setMessage(this.getResources().getString(R.string.dialog_delete_message))
				 .setCancelable(false)
				 .setPositiveButton(this.getResources().getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 FileCommander.this.deleteSelectedFile();
					 }
				 })
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
			case MOVE_DIALOG:
				 builder.setTitle(this.getResources().getString(R.string.dialog_move_title));
				 builder.setMessage(this.getResources().getString(R.string.dialog_move_message))
				 .setCancelable(false)
				 .setPositiveButton(this.getResources().getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 FileCommander.this.moveFileHere();
					 }
				 })
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
			case COPY_DIALOG:
				 LayoutInflater inf = this.getLayoutInflater();
				 View cpdialog = inf.inflate(R.layout.copydialog, null);
				 this.mCopyDialogText = (TextView) cpdialog.findViewById(R.id.textViewCopy);
				 this.mCopyDialogProBar = (ProgressBar) cpdialog.findViewById(R.id.progressBarCopy);
				 builder.setTitle(this.getResources().getString(R.string.dialog_copy_title));
				 builder.setView(cpdialog);
				 builder.setCancelable(false)
				 .setNegativeButton(this.getResources().getString(R.string.dialog_CANCEL), new DialogInterface.OnClickListener() {
					 public void onClick(DialogInterface dialog, int id) {
						 if(FileCommander.this.cp != null){
							 FileCommander.this.cp.cancel(true);
						 }
						 dialog.cancel();
					 }
				 });
				 dialog = builder.create();
				 return dialog;
		}
		return super.onCreateDialog(id);
	}
	
	
	//*************************************************************************
	// Set current directory to external Storage Directory
	//*************************************************************************
	
	private void setSDRoot(){
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	    	mFSAdapter.relocateToRoot(path);
	    	mPath= mFSAdapter.currentPath();
		}
	}

	
	//*************************************************************************
	// Restore data from previous instance
	//*************************************************************************
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		this.mPath = savedInstanceState.getString("PATH");
		if(mPath != null){
			mFSAdapter.relocateToRoot(mPath);
		}
		this.mFileSourcePath = savedInstanceState.getString("SOURCEPATH");
		this.mLastSelectedItem=savedInstanceState.getInt("LASTSELECTEDITEM");
		this.mDeleteModeActive=savedInstanceState.getBoolean("DELETEMODEACTIVE",false);
		this.mMoveModeActive=savedInstanceState.getBoolean("MOVEMODEACTIVE",false);
		this.mCopyModeActive=savedInstanceState.getBoolean("COPYMODEACTIVE",false);
	}

	
	//*************************************************************************
	// Save important data when instance gets destroyed
	//*************************************************************************
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPath= mFSAdapter.currentPath();
		outState.putString("PATH",mPath );
		outState.putString("SOURCEPATH",mFileSourcePath );
		outState.putInt("LASTSELECTEDITEM", this.mLastSelectedItem);
		outState.putBoolean("DELETEMODEACTIVE", this.mDeleteModeActive);
		outState.putBoolean("MOVEMODEACTIVE", this.mMoveModeActive);
		outState.putBoolean("COPYMODEACTIVE", this.mCopyModeActive);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		loadPreferences();
		setDeleteButtonState();
		setMoveButtonState();
		super.onResume();
	}
    
	
	//*************************************************************************
	// load Preferences
	//*************************************************************************
	
	private void loadPreferences(){
		mPreferenceDeleteConfirmation=mSharedPref.getBoolean("preferenceCheckboxFastDeleteConfirmation", true);
	}
	
	
	//*************************************************************************
	// Create a new file in the current directory
	//*************************************************************************
	
	public void createNewFile(String filename){
		File newFile = new File(mFSAdapter.currentPath(), filename);
		try {
			if(newFile.createNewFile()){
				Toast.makeText(this, this.getResources().getString(R.string.operation_success), Toast.LENGTH_LONG).show();
				reloadAdapterData();
			}
			else {
				Toast.makeText(this, this.getResources().getString(R.string.operation_failed), Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
			Toast.makeText(this, "file creation failed with IOException", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	
	//*************************************************************************
	// Create a new directory in the current directory
	//*************************************************************************
	
	public void createNewDir(String dirname){
		File newDir = new File(mFSAdapter.currentPath(), dirname);
		if(newDir.mkdir()){
			Toast.makeText(this, this.getResources().getString(R.string.operation_success), Toast.LENGTH_LONG).show();
			reloadAdapterData();
		}
		else {
			Toast.makeText(this, this.getResources().getString(R.string.operation_failed), Toast.LENGTH_LONG).show();
		}
	}
	

	
	//*************************************************************************
	// Rename file or directory to newname
	//*************************************************************************
	
	public void renameSelectedFile(String newName){
		if(mFSAdapter!= null){
			File renFile = new File(mFSAdapter.getPath(mLastSelectedItem));
			renFile.renameTo(new File(mFSAdapter.currentPath()+"/"+newName));
			reloadAdapterData();
		}
	}
	
	
	//*************************************************************************
	// delete file or directory
	//*************************************************************************
	
	public void deleteSelectedFile(String path){
		if(mFSAdapter!= null){
			File delFile = new File(path);
			if(delFile.isDirectory()){
				File[] dirContent=delFile.listFiles();
				if(dirContent!=null){
					for(File files:dirContent){
						deleteSelectedFile(files.getAbsolutePath());
					}
				}
			}
			delFile.delete();
			reloadAdapterData();
		}
	}
	
	public void deleteSelectedFile(){
		if(mFSAdapter!= null){
			deleteSelectedFile(mFSAdapter.getPath(mLastSelectedItem));
		}
	}
	
	
	//*************************************************************************
	// view file 
	//*************************************************************************
	
	public void viewSelectedFile(String path){
		if(mFSAdapter!= null){
			  File vFile = new File(path);
			  Intent intent = new Intent(Intent.ACTION_VIEW);
			  String mime = URLConnection.guessContentTypeFromName (vFile.getAbsolutePath());
			  intent.setDataAndType(Uri.fromFile(vFile),mime);
			  try{
				  startActivity(intent);
			  }
			  
			  catch(ActivityNotFoundException e){
				  Toast.makeText(FileCommander.this, FileCommander.this.getResources().getString(R.string.exception_unknowFileType), Toast.LENGTH_LONG).show();
			  }
			
		}
	}
	
	
	public void openSelectedFile(){
		if(mFSAdapter!= null){
			if(mFSAdapter.relocateToPosition(mLastSelectedItem)){
				mPath= mFSAdapter.currentPath();
			}
			else {
				FileCommander.this.viewSelectedFile(mFSAdapter.getPath(mLastSelectedItem));
			}
		}
	}
	
	
	//*************************************************************************
	// send file 
	//*************************************************************************
	
	public void sendSelectedFile(String path){
		if(mFSAdapter!= null){
			  File sendFile = new File(path);
			  Intent intent = new Intent(Intent.ACTION_SEND);
			  String mime = URLConnection.guessContentTypeFromName (sendFile.getAbsolutePath());
			  intent.setType(mime);
			  intent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(sendFile));
			  try{
				  startActivity(Intent.createChooser(intent, "Send"));
			  }
			  catch(ActivityNotFoundException e){
				  Toast.makeText(FileCommander.this, this.getResources().getString(R.string.exception_unknowFileType), Toast.LENGTH_LONG).show();
			  }
			
		}
	}
	
	
	public void sendSelectedFile(){
		if(mFSAdapter!= null){
			sendSelectedFile(mFSAdapter.getPath(mLastSelectedItem));
		}
	}
	
	
	//*************************************************************************
	// move file
	//*************************************************************************
	
	public void selectMoveFile(){
		if(mFSAdapter!= null){
			mFileSourcePath=mFSAdapter.getPath(mLastSelectedItem);
			mMoveModeActive=true;
			mCopyModeActive=false;
			setMoveButtonState();
		}
	}

	public void moveFileHere(){
		if(mFSAdapter!= null && mFileSourcePath!=null){
			File oldFile = new File(mFileSourcePath);
			boolean success=oldFile.renameTo(new File(mFSAdapter.currentPath()+"/"+oldFile.getName()));
			if(success){
				mMoveModeActive=false;
				setMoveButtonState();
				reloadAdapterData();
			}
			else {
				Toast.makeText(this, this.getResources().getString(R.string.operation_failed), Toast.LENGTH_LONG).show();
			}
			
		}
	}

	
	//*************************************************************************
	// copy file
	//*************************************************************************
	
	private void selectCopyFile() {
		if(mFSAdapter!= null){
			mFileSourcePath=mFSAdapter.getPath(mLastSelectedItem);
			mCopyModeActive=true;
			mMoveModeActive=false;
			setMoveButtonState();
		}
	}
	
	
	public void copyFileHere(){
		if(mFSAdapter!= null && mFileSourcePath!=null){
			File oldFile = new File(mFileSourcePath);
			if(oldFile.isFile()){
				cp = new CopyTask();
				cp.execute(mFileSourcePath,mFSAdapter.currentPath()+"/"+oldFile.getName());
			}
			else {
				dismissDialog(COPY_DIALOG);
				reloadAdapterData();
			}
			mCopyModeActive=false;
			setMoveButtonState();
		}
	}
	
	
	//*************************************************************************
	// change the background image of the delete button when it is clicked
	//*************************************************************************
	
	private void setDeleteButtonState(){
		if(mDeleteModeActive){
        	mDeleteButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_deletered80);
        }
		else {
			mDeleteButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_deleteblack80);
		}
	}
	
	
	//*************************************************************************
	// change the background image of the move/copy button when it is available
	//*************************************************************************
	
	private void setMoveButtonState(){
		if(mMoveModeActive || mCopyModeActive){
			mMoveButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_movedest80);
			mMoveButtonImg.setClickable(true);
        }
		else {
			mMoveButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_movedest_inactive);
			mMoveButtonImg.setClickable(false);
		}
	}
	
	
	//*************************************************************************
	// Setup click listeners for top push buttons and item Listener for Grid
	//*************************************************************************
	
	private void setupButtonListeners(){
		
		
		// Back button move dir back
		mBackButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					mFSAdapter.moveUp();
					mPath= mFSAdapter.currentPath();
				}
			}
		};
		mBackButtonImg.setOnClickListener(mBackButtonClicked);
		
		
		// Home button
		mHomeButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					FileCommander.this.setSDRoot();
				}
			}
		};
		mHomeButtonImg.setOnClickListener(mHomeButtonClicked);
		
		
		// Reload button
		mReloadButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				reloadAdapterData();
			}
		};
		mReloadButtonImg.setOnClickListener(mReloadButtonClicked);
		
		
		// File plus button create new file
		mFilePlusButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					FileCommander.this.showDialog(FILECREATE_DIALOG);
				}
			}
		};
		mFilePlusButtonImg.setOnClickListener(mFilePlusButtonClicked);
		
		
		// Dir plus button create new dir
		mDirPlusButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					FileCommander.this.showDialog(DIRCREATE_DIALOG);
				}
			}
		};
		mDirPlusButtonImg.setOnClickListener(mDirPlusButtonClicked);
		
		
		// move to/copy to button 
		mMoveButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					if(mMoveModeActive){
						FileCommander.this.showDialog(MOVE_DIALOG);
					}
					else if (mCopyModeActive){
						FileCommander.this.showDialog(COPY_DIALOG);
						FileCommander.this.copyFileHere();
					}
				}
			}
		};
		mMoveButtonImg.setOnClickListener(mMoveButtonClicked);
		
		
		//Delete mode button clicked
		mDeleteButtonClicked= new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					mDeleteModeActive = !mDeleteModeActive;
					if(mDeleteModeActive){
						Toast.makeText(FileCommander.this, FileCommander.this.getResources().getString(R.string.activation_fastDelete), Toast.LENGTH_LONG).show();
					}
					setDeleteButtonState();
				}
			}
		};
		mDeleteButtonImg.setOnClickListener(mDeleteButtonClicked);
		
		// Grid view item clicked -> open directory if its a dir
		mGridView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				if(FileCommander.this.mDeleteModeActive){
					if(mPreferenceDeleteConfirmation){
						//Fast delete with confirmation dialog
						mLastSelectedItem=position;
						showDialog(DELETE_DIALOG);
					}
					else {
						//Fast delete without confirmation
						FileCommander.this.deleteSelectedFile(mFSAdapter.getPath(position));
					}
					
				}
				else{
					if(mFSAdapter.relocateToPosition(position)){
						mPath= mFSAdapter.currentPath();
					}
					else {
						FileCommander.this.viewSelectedFile(mFSAdapter.getPath(position));
					}
				}
			}
		}
		);
		
	}

	
	//*************************************************************************
	// Refresh Adapter data
	//*************************************************************************
	
	private void reloadAdapterData(){
		if(mFSAdapter != null){
			mFSAdapter.reloadCurrentDir();
		}
	}
	
	
	//*************************************************************************
	// start preferenc activity
	//*************************************************************************
	
	private void showPreferenceScreen(){
		Intent intent = new Intent(this,Preferences.class);
		this.startActivity(intent);
	}
	
	
	//*************************************************************************
	// Observer gets called when adapter data has changed, this could mean we  
	// need to refresh the displayed path
	//*************************************************************************
	
	private void setupObserver(){
		mDObserver = new DataSetObserver(){
			@Override
			public void onChanged() {
				mPathLine.setText(mFSAdapter.currentPath());
				super.onChanged();
			}
			
		};
		if(mFSAdapter!=null){
			mFSAdapter.registerDataSetObserver(mDObserver);
		}
	}
	
	
	//*************************************************************************
	// Copytask, copying is handled asynchronously 
	// a Progress Dialog in the FileCommander is updated from onProgressUpdate
	//*************************************************************************
	
	private class CopyTask extends AsyncTask<String,Integer,Integer>{

		@Override
		protected void onPostExecute(Integer result) {
			dismissDialog(COPY_DIALOG);
			reloadAdapterData();
		}

		@Override
		protected void onPreExecute() {
			showDialog(COPY_DIALOG);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mCopyDialogText!=null && mCopyDialogProBar!=null){
				mCopyDialogText.setText(values[0].toString()+"%");
				mCopyDialogProBar.setProgress(values[0]);
			}
		}

		@Override
		protected Integer doInBackground(String... paths) {
			if(paths.length==2){
				try {
					int count=0;
					long total=0,sourceSize=0;
					byte[] buffer = new byte[8192];
					File sourceFile = new File(paths[0]);
					sourceSize=sourceFile.length();
					BufferedInputStream bufinp=new BufferedInputStream(new FileInputStream(sourceFile));
					BufferedOutputStream outFile=new BufferedOutputStream(new FileOutputStream(new File(paths[1])));
					while (((count = bufinp.read(buffer)) != -1) && (!this.isCancelled())){
						outFile.write(buffer,0,count);
						total+=count;
						publishProgress((int) ((((float)total/sourceSize))*100));
					} 
					bufinp.close();
					outFile.close();
					if(this.isCancelled()){
						return 3;
					}
				}catch(Exception e){
					return 2;
				}
			  return 0;
			}
			return 1;
		}
		
	}
	
	
}