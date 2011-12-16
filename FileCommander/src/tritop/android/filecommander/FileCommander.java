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

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import android.widget.TextView;
import android.widget.Toast;

public class FileCommander extends Activity {
    private final static int FILECREATE_DIALOG = 510;
    private final static int DIRCREATE_DIALOG = 511;
    private final static int RENAME_DIALOG = 512;
    private final static int DELETE_DIALOG = 513;
    private final static int MOVE_DIALOG = 514;
    
    private String mPath=null;
	private boolean mDeleteModeActive=false;
	private boolean mMoveModeActive=false;
	private int mLastSelectedItem=0;
	private String mFileMovePath;
	private GridView mGridView;
	private TextView mPathLine;
	private StorageViewAdapter mFSAdapter;
	private OnClickListener mBackButtonClicked,mHomeButtonClicked,mReloadButtonClicked,mFilePlusButtonClicked,mDirPlusButtonClicked,mMoveButtonClicked,mDeleteButtonClicked;
	private ImageView mBackButtonImg,mHomeButtonImg,mReloadButtonImg,mFilePlusButtonImg,mDirPlusButtonImg,mMoveButtonImg,mDeleteButtonImg;
	private DataSetObserver mDObserver;
	
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
        
        mFSAdapter = new StorageViewAdapter(this);
    	mGridView.setAdapter(mFSAdapter);
    	this.registerForContextMenu(mGridView);
    	setupObserver();
    	setupButtonListeners();
    	setSDRoot();
    	setDeleteButtonBackground();
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
		mPath = savedInstanceState.getString("PATH");
		if(mPath != null){
			mFSAdapter.relocateToRoot(mPath);
		}
		this.mDeleteModeActive=savedInstanceState.getBoolean("DELETEMODEACTIVE",false);
		this.mLastSelectedItem=savedInstanceState.getInt("LASTSELECTEDITEM");
		this.mMoveModeActive=savedInstanceState.getBoolean("MOVEMODEACTIVE");
	}

	
	//*************************************************************************
	// Save important data when instance gets destroyed
	//*************************************************************************
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPath= mFSAdapter.currentPath();
		outState.putString("PATH",mPath );
		outState.putBoolean("DELETEMODEACTIVE", this.mDeleteModeActive);
		outState.putInt("LASTSELECTEDITEM", this.mLastSelectedItem);
		outState.putBoolean("MOVEMODEACTIVE", this.mMoveModeActive);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		setDeleteButtonBackground();
		setMoveButtonState();
		super.onResume();
	}
    
	
	//*************************************************************************
	// Create a new file in the current directory
	//*************************************************************************
	
	public void createNewFile(String filename){
		File newFile = new File(mFSAdapter.currentPath(), filename);
		try {
			if(newFile.createNewFile()){
				Toast.makeText(this, this.getResources().getString(R.string.operation_success), Toast.LENGTH_LONG).show();
				mFSAdapter.reloadCurrentDir();
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
			mFSAdapter.reloadCurrentDir();
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
			mFSAdapter.reloadCurrentDir();
		}
	}
	
	
	//*************************************************************************
	// delete file or directory
	//*************************************************************************
	
	public void deleteSelectedFile(String path){
		if(mFSAdapter!= null){
			File renFile = new File(path);
			renFile.delete();
			mFSAdapter.reloadCurrentDir();
		}
	}
	
	public void deleteSelectedFile(){
		if(mFSAdapter!= null){
			deleteSelectedFile(mFSAdapter.getPath(mLastSelectedItem));
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
			mFileMovePath=mFSAdapter.getPath(mLastSelectedItem);
			mMoveModeActive=true;
			setMoveButtonState();
		}
	}

	public void moveFileHere(){
		if(mFSAdapter!= null && mFileMovePath!=null){
			File oldFile = new File(mFileMovePath);
			boolean success=oldFile.renameTo(new File(mFSAdapter.currentPath()+"/"+oldFile.getName()));
			if(success){
				mMoveModeActive=false;
				setMoveButtonState();
				mFSAdapter.reloadCurrentDir();
			}
			else {
				Toast.makeText(this, this.getResources().getString(R.string.operation_failed), Toast.LENGTH_LONG).show();
			}
			
		}
	}

	//*************************************************************************
	// change the background image of the delete button when it is clicked
	//*************************************************************************
	
	private void setDeleteButtonBackground(){
		if(mDeleteModeActive){
        	mDeleteButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_deletered80);
        }
		else {
			mDeleteButtonImg.setBackgroundResource(R.drawable.blue_glossy_btnbase2_deleteblack80);
		}
	}
	
	//*************************************************************************
	// change the background image of the move button when it is available
	//*************************************************************************
	
	private void setMoveButtonState(){
		if(mMoveModeActive){
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
				if(mFSAdapter != null){
					mFSAdapter.reloadCurrentDir();
				}
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
		
		
		// moveto button create new dir
		mMoveButtonClicked = new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mFSAdapter != null){
					FileCommander.this.showDialog(MOVE_DIALOG);
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
					setDeleteButtonBackground();
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
					FileCommander.this.deleteSelectedFile(mFSAdapter.getPath(position));
				}
				else{
					if(mFSAdapter.relocateToPosition(position)){
						mPath= mFSAdapter.currentPath();
					}
					else {
					  File vFile = new File(mFSAdapter.getPath(position));
					  Intent intent = new Intent(Intent.ACTION_VIEW);
					  String mime = URLConnection.guessContentTypeFromName (vFile.getAbsolutePath());
					  Log.e("File Commander","mimetype "+mime);
					  intent.setDataAndType(Uri.fromFile(vFile),mime);
					  try{
						  startActivity(intent);
					  }
					  
					  catch(ActivityNotFoundException e){
						  Toast.makeText(FileCommander.this, FileCommander.this.getResources().getString(R.string.exception_unknowFileType), Toast.LENGTH_LONG).show();
					  }
					}
				}
			}
		}
		);
		
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
	
}